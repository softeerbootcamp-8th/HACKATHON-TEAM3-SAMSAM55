package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.Participant;

public record InviteParticipantDto(Long participantId, String roleName, boolean joined) {

    public static InviteParticipantDto from(Participant participant) {
        return new InviteParticipantDto(
                participant.getId(),
                participant.getRoleName(),
                participant.getJoinedAt() != null
        );
    }
}
