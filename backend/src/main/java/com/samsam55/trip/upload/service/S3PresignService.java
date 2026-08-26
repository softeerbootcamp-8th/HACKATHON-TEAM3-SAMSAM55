package com.samsam55.trip.upload.service;

import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.upload.dto.PresignedUrlResponseDto;
import com.samsam55.trip.upload.exception.UploadErrorType;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * 선택지 사진을 S3에 직접(presigned URL로) 올리기 위한 URL 발급을 담당한다. 실제 파일 바이트는
 * 서버를 거치지 않고 클라이언트가 발급받은 URL로 S3에 직접 PUT한다. 버킷은 퍼블릭 읽기로
 * 열어뒀으므로(별도 정책) 공개 URL을 그대로 이미지 주소로 쓴다 — 업로드 여부를 서버가 추적하는
 * 세션 같은 건 두지 않는다(해커톤 규모라 최소로만 구현).
 */
@Service
@RequiredArgsConstructor
public class S3PresignService {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp");

    private final S3Presigner presigner;
    private final UploadProperties uploadProperties;

    /**
     * fileName의 확장자로 새 S3 key를 만들고, 그 key로 직접 PUT할 수 있는 presigned URL을 발급한다.
     * key에는 원본 파일명을 넣지 않는다 — 한글 등 비-ASCII 파일명이 S3 key/URL 인코딩 문제를 일으킬
     * 수 있고, 이후 어디서도 원본 파일명을 다시 쓰지 않기 때문이다. Content-Type도 서명에 포함하지
     * 않는다 — 서명에 포함하면 실제 업로드 시점에 브라우저가 보내는 Content-Type과 정확히 일치해야만
     * S3가 요청을 받아주는데, 파일에 따라 브라우저가 판단하는 값이 달라 업로드가 종종 실패했다.
     *
     * @param fileName 업로드할 파일의 원래 이름(확장자 판별에만 쓰고 key에는 포함하지 않는다)
     * @return 업로드용 presigned URL, key, 업로드 후 보여줄 공개 URL
     * @throws ApplicationException fileName이 비어있거나 경로 구분자가 섞여 있을 때(INVALID_FILE_NAME)
     * @throws ApplicationException 지원하지 않는 확장자일 때(UNSUPPORTED_FILE_TYPE)
     */
    public PresignedUrlResponseDto issueUploadUrl(String fileName) {
        validateFileName(fileName);
        String extension = resolveExtension(fileName);

        String key = "uploads/vote-options/" + UUID.randomUUID() + "." + extension;

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(uploadProperties.bucketName())
                .key(key)
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(objectRequest)
                .build();
        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);

        return new PresignedUrlResponseDto(presignedRequest.url().toString(), key, toPublicUrl(key));
    }

    /**
     * key가 가리키는 객체의 공개 URL을 만든다. key가 없으면(이미지를 등록하지 않은 선택지) null을 반환한다.
     */
    public String toPublicUrl(String key) {
        if (key == null) {
            return null;
        }
        try {
            String host = uploadProperties.bucketName() + ".s3." + uploadProperties.region() + ".amazonaws.com";
            return new URI("https", host, "/" + key, null).toASCIIString();
        } catch (URISyntaxException e) {
            throw new ApplicationException(UploadErrorType.INVALID_FILE_NAME);
        }
    }

    /**
     * fileName이 비어있으면 첨부 자체가 없는 것으로 보고 거부한다. 경로 구분자/상위 디렉토리 참조가
     * 섞여있으면 S3 key에 그대로 이어붙이므로 의도하지 않은 key 경로가 만들어질 수 있어 거부한다.
     */
    private void validateFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new ApplicationException(UploadErrorType.INVALID_FILE_NAME);
        }
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            throw new ApplicationException(UploadErrorType.INVALID_FILE_NAME);
        }
    }

    private String resolveExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            throw new ApplicationException(UploadErrorType.UNSUPPORTED_FILE_TYPE);
        }
        String extension = fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new ApplicationException(UploadErrorType.UNSUPPORTED_FILE_TYPE);
        }
        return extension;
    }
}
