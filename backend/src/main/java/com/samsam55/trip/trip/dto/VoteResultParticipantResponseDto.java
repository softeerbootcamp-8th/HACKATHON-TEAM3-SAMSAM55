package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.Participant;

public record VoteResultParticipantResponseDto(
        Long participantId,
        String roleName
) {

    public static VoteResultParticipantResponseDto from(Participant participant) {
        return new VoteResultParticipantResponseDto(
                participant.getId(),
                participant.getRoleName()
        );
    }
}
