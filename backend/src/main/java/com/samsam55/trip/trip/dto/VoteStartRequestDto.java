package com.samsam55.trip.trip.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record VoteStartRequestDto(
        @NotEmpty(message = "투표를 시작할 일정 항목을 1개 이상 선택해야 합니다.")
        List<Long> itemIds
) {
}
