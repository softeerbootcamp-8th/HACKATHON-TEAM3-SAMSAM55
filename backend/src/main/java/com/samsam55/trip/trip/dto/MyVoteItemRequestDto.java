package com.samsam55.trip.trip.dto;

import jakarta.validation.constraints.NotNull;

public record MyVoteItemRequestDto(
        @NotNull(message = "투표할 일정 항목을 지정해야 합니다.")
        Long itemId,
        @NotNull(message = "선택할 옵션을 지정해야 합니다.")
        Long voteOptionId
) {
}
