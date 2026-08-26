package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.Participant;

public record VoteStatusParticipantResponseDto(
        Long participantId,
        String roleName,
        boolean voted
) {

    public static VoteStatusParticipantResponseDto of(Participant participant, boolean voted) {
        return new VoteStatusParticipantResponseDto(participant.getId(), participant.getRoleName(), voted);
    }
}
