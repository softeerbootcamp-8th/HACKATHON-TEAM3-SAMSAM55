package com.samsam55.trip.trip.dto;

import jakarta.validation.constraints.NotNull;

public record ItineraryItemConfirmRequestDto(
        @NotNull(message = "확정할 선택지를 지정해야 합니다.")
        Long voteOptionId
) {
}
