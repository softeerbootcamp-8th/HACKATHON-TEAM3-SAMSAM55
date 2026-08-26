package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.ItineraryItem;
import com.samsam55.trip.trip.entity.Participant;
import java.time.LocalDate;
import java.util.List;

public record VoteResultResponseDto(
        Long itemId,
        String name,
        String category,
        String status,
        Integer dayNumber,
        LocalDate date,
        int totalParticipants,
        int votedCount,
        int optionCount,
        Long confirmedOptionId,
        List<VoteResultParticipantResponseDto> participants,
        List<VoteResultParticipantResponseDto> pendingParticipants,
        List<VoteResultOptionResponseDto> options
) {

    public static VoteResultResponseDto of(
            ItineraryItem item,
            List<Participant> participants,
            List<Participant> pendingParticipants,
            List<VoteResultOptionResponseDto> options
    ) {
        return new VoteResultResponseDto(
                item.getId(),
                item.getName(),
                item.getCategory(),
                item.getStatus().name(),
                item.getTripDay().getDayNumber(),
                item.getTripDay().getTripDate(),
                participants.size(),
                participants.size() - pendingParticipants.size(),
                options.size(),
                item.getConfirmedOption() == null ? null : item.getConfirmedOption().getId(),
                participants.stream()
                        .map(VoteResultParticipantResponseDto::from)
                        .toList(),
                pendingParticipants.stream()
                        .map(VoteResultParticipantResponseDto::from)
                        .toList(),
                options
        );
    }
}
