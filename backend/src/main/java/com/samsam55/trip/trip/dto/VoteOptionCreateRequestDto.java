package com.samsam55.trip.trip.dto;

import jakarta.validation.constraints.NotBlank;

public record VoteOptionCreateRequestDto(
        @NotBlank(message = "선택지 이름은 필수입니다.")
        String name,

        // presigned URL로 미리 업로드한 사진의 S3 key. 사진을 첨부하지 않았으면 null.
        String imageKey
) {
}
