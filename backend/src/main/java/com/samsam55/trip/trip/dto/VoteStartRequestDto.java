package com.samsam55.trip.trip.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record VoteStartRequestDto(
        @NotEmpty(message = "일정 항목 ID 목록은 필수입니다.")
        List<Long> itemIds
) {
}
