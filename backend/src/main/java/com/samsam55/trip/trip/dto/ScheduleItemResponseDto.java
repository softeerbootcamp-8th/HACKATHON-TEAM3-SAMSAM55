package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.ItineraryItem;

public record ScheduleItemResponseDto(
        Long id,
        String name,
        String category,
        String decisionType,
        String status,
        Integer sortOrder,
        int votedCount,
        long totalParticipants,
        ScheduleConfirmedOptionResponseDto confirmedOption
) {

    public static ScheduleItemResponseDto of(
            ItineraryItem item,
            int votedCount,
            long totalParticipants
    ) {
        return new ScheduleItemResponseDto(
                item.getId(),
                item.getName(),
                item.getCategory(),
                item.getDecisionType().name(),
                item.getStatus().name(),
                item.getSortOrder(),
                votedCount,
                totalParticipants,
                ScheduleConfirmedOptionResponseDto.from(item.getConfirmedOption())
        );
    }
}
