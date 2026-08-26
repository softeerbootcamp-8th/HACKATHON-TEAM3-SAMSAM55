package com.samsam55.trip.upload.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.upload.dto.PresignedUrlResponseDto;
import com.samsam55.trip.upload.exception.UploadErrorType;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@ExtendWith(MockitoExtension.class)
class S3PresignServiceTest {

    @Mock
    private S3Presigner presigner;

    @Mock
    private PresignedPutObjectRequest presignedRequest;

    private final UploadProperties uploadProperties =
            new UploadProperties("samsam55-trip-images", "ap-northeast-2");

    private S3PresignService s3PresignService;

    @BeforeEach
    void setUp() {
        s3PresignService = new S3PresignService(presigner, uploadProperties);
    }

    @Test
    @DisplayName("한글이 섞인 파일명이어도 key에는 UUID와 확장자만 들어간다")
    void 한글이_섞인_파일명이어도_key에는_UUID와_확장자만_들어간다() throws Exception {
        when(presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedRequest);
        when(presignedRequest.url()).thenReturn(new URI("https://example.com/signed").toURL());

        PresignedUrlResponseDto response = s3PresignService.issueUploadUrl("여행 사진.jpg");

        assertThat(response.key()).matches("uploads/vote-options/[0-9a-fA-F-]{36}\\.jpg");
        assertThat(response.key()).doesNotContain("여행");
        assertThat(response.imageUrl())
                .isEqualTo("https://samsam55-trip-images.s3.ap-northeast-2.amazonaws.com/" + response.key());
    }

    @Test
    @DisplayName("지원하지 않는 확장자면 예외가 발생한다")
    void 지원하지_않는_확장자면_예외가_발생한다() {
        assertThatThrownBy(() -> s3PresignService.issueUploadUrl("photo.heic"))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(UploadErrorType.UNSUPPORTED_FILE_TYPE));
    }

    @Test
    @DisplayName("확장자가 없으면 예외가 발생한다")
    void 확장자가_없으면_예외가_발생한다() {
        assertThatThrownBy(() -> s3PresignService.issueUploadUrl("photo"))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(UploadErrorType.UNSUPPORTED_FILE_TYPE));
    }

    @Test
    @DisplayName("파일명이 비어있으면 예외가 발생한다")
    void 파일명이_비어있으면_예외가_발생한다() {
        assertThatThrownBy(() -> s3PresignService.issueUploadUrl(""))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(UploadErrorType.INVALID_FILE_NAME));
    }

    @Test
    @DisplayName("파일명에 경로 구분자가 섞여 있으면 예외가 발생한다")
    void 파일명에_경로_구분자가_섞여_있으면_예외가_발생한다() {
        assertThatThrownBy(() -> s3PresignService.issueUploadUrl("../secret.jpg"))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(UploadErrorType.INVALID_FILE_NAME));
    }

    @Test
    @DisplayName("key가 없으면 공개 URL은 null이다")
    void key가_없으면_공개_URL은_null이다() {
        assertThat(s3PresignService.toPublicUrl(null)).isNull();
    }

    @Test
    @DisplayName("key로 공개 URL을 만든다")
    void key로_공개_URL을_만든다() {
        String url = s3PresignService.toPublicUrl("uploads/vote-options/abc.jpg");

        assertThat(url)
                .isEqualTo("https://samsam55-trip-images.s3.ap-northeast-2.amazonaws.com/uploads/vote-options/abc.jpg");
    }
}
