package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.Trip;
import java.util.List;

public record TripListResponseDto(List<TripSummaryResponseDto> items) {

    public static TripListResponseDto from(List<Trip> trips) {
        return new TripListResponseDto(
                trips.stream()
                        .map(TripSummaryResponseDto::from)
                        .toList()
        );
    }
}
