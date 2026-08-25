package com.samsam55.trip.trip.dto;

import java.util.List;

public record VoteStatusResponseDto(
        int votedCount,
        int totalParticipants,
        List<VoteStatusParticipantResponseDto> participants,
        List<VoteStatusOptionResponseDto> options
) {
}
