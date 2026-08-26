package com.samsam55.trip.upload.controller;

import com.samsam55.trip.auth.annotation.Login;
import com.samsam55.trip.global.common.CommonResponse;
import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.upload.dto.PresignedUrlResponseDto;
import com.samsam55.trip.upload.service.S3PresignService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UploadController {

    private final S3PresignService s3PresignService;

    /**
     * 선택지 사진을 S3에 직접 올릴 수 있는 presigned URL을 발급한다. 여행 방장만 호출할 수 있다.
     *
     * @param loginUserId 로그인한 회원의 식별자
     * @param fileName 업로드할 파일의 원래 이름(확장자 포함)
     * @return 업로드용 presigned URL, key, 공개 URL이 담긴 200 응답
     * @throws ApplicationException fileName이 비어있거나 올바르지 않을 때(INVALID_FILE_NAME)
     * @throws ApplicationException 지원하지 않는 확장자일 때(UNSUPPORTED_FILE_TYPE)
     */
    @GetMapping("/api/uploads/presigned-url")
    public CommonResponse<PresignedUrlResponseDto> getPresignedUrl(
            @Login Long loginUserId,
            @RequestParam String fileName
    ) {
        return CommonResponse.success(s3PresignService.issueUploadUrl(fileName));
    }
}
