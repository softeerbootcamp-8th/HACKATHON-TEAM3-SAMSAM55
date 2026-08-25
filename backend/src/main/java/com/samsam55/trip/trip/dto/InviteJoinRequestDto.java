package com.samsam55.trip.trip.dto;

import jakarta.validation.constraints.NotNull;

public record InviteJoinRequestDto(
        @NotNull(message = "participantId는 필수입니다.")
        Long participantId
) {
}
