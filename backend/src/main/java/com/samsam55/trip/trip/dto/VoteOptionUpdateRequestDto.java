package com.samsam55.trip.trip.dto;

import jakarta.validation.constraints.NotBlank;

public record VoteOptionUpdateRequestDto(
        @NotBlank(message = "선택지 이름은 필수입니다.")
        String name,

        String description,

        // presigned URL로 미리 업로드한 새 사진의 S3 key. null이면 기존 이미지를 그대로 유지한다.
        String imageKey
) {
}
