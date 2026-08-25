package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.Participant;

public record InviteJoinResponseDto(Long participantId, Long tripId, String roleName) {

    public static InviteJoinResponseDto from(Participant participant) {
        return new InviteJoinResponseDto(
                participant.getId(),
                participant.getTrip().getId(),
                participant.getRoleName()
        );
    }
}
