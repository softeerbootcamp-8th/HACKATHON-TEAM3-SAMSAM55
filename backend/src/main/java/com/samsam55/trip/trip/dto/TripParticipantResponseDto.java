package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.Participant;

public record TripParticipantResponseDto(Long participantId, String roleName) {

    public static TripParticipantResponseDto from(Participant participant) {
        return new TripParticipantResponseDto(participant.getId(), participant.getRoleName());
    }
}
