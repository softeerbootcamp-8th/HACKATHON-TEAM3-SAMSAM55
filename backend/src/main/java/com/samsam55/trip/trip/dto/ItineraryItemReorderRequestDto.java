package com.samsam55.trip.trip.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ItineraryItemReorderRequestDto(
        @NotEmpty(message = "순서를 변경할 일정 항목 목록은 필수입니다.")
        List<Long> itemIds
) {
}
