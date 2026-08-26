package com.samsam55.trip.trip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ItineraryItemBasicInfoUpdateRequestDto(
        @NotBlank(message = "일정 이름은 필수입니다.")
        @Size(max = 100, message = "일정 이름은 100자 이하여야 합니다.")
        String name,

        @Size(max = 50, message = "카테고리는 50자 이하여야 합니다.")
        String category
) {
}
