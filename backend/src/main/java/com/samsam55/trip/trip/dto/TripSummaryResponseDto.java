package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.Trip;
import java.time.LocalDate;

public record TripSummaryResponseDto(
        Long id,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        Integer companionCount
) {

    public static TripSummaryResponseDto from(Trip trip) {
        return new TripSummaryResponseDto(
                trip.getId(),
                trip.getTitle(),
                trip.getStartDate().toLocalDate(),
                trip.getEndDate().toLocalDate(),
                trip.getCompanionCount()
        );
    }
}
