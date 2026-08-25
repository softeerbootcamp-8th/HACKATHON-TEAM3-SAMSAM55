package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.Participant;
import com.samsam55.trip.trip.entity.Trip;
import java.time.LocalDate;
import java.util.List;

public record TripCreateResponseDto(
        Long id,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        Integer companionCount,
        String inviteCode,
        List<TripParticipantResponseDto> participants
) {

    public static TripCreateResponseDto from(Trip trip, List<Participant> participants) {
        return new TripCreateResponseDto(
                trip.getId(),
                trip.getTitle(),
                trip.getStartDate().toLocalDate(),
                trip.getEndDate().toLocalDate(),
                trip.getCompanionCount(),
                trip.getInviteCode(),
                participants.stream()
                        .map(TripParticipantResponseDto::from)
                        .toList()
        );
    }
}
