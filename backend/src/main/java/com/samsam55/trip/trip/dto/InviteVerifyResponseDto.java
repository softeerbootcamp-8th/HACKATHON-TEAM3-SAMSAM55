package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.Participant;
import com.samsam55.trip.trip.entity.Trip;
import java.util.List;

public record InviteVerifyResponseDto(Long tripId, String title, List<InviteParticipantDto> participants) {

    public static InviteVerifyResponseDto of(Trip trip, List<Participant> participants) {
        List<InviteParticipantDto> participantDtos = participants.stream()
                .map(InviteParticipantDto::from)
                .toList();
        return new InviteVerifyResponseDto(trip.getId(), trip.getTitle(), participantDtos);
    }
}
