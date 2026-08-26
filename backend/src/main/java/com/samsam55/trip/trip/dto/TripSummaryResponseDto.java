package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.Trip;
import java.time.LocalDate;

public record TripSummaryResponseDto(
        Long id,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        Integer companionCount,
        long totalItems,
        long confirmedItems,
        int progressPercent
) {

    public static TripSummaryResponseDto from(Trip trip, long totalItems, long confirmedItems) {
        return new TripSummaryResponseDto(
                trip.getId(),
                trip.getTitle(),
                trip.getStartDate().toLocalDate(),
                trip.getEndDate().toLocalDate(),
                trip.getCompanionCount(),
                totalItems,
                confirmedItems,
                calculateProgressPercent(totalItems, confirmedItems)
        );
    }

    private static int calculateProgressPercent(long totalItems, long confirmedItems) {
        if (totalItems == 0) {
            return 0;
        }
        return (int) (confirmedItems * 100 / totalItems);
    }
}
