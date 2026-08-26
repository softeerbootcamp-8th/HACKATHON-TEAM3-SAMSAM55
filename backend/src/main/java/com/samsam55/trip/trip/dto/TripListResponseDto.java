package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.Trip;
import java.util.List;
import java.util.Map;

public record TripListResponseDto(List<TripSummaryResponseDto> items) {

    public static TripListResponseDto from(
            List<Trip> trips,
            Map<Long, Long> totalItemsByTripId,
            Map<Long, Long> confirmedItemsByTripId
    ) {
        return new TripListResponseDto(
                trips.stream()
                        .map(trip -> TripSummaryResponseDto.from(
                                trip,
                                totalItemsByTripId.getOrDefault(trip.getId(), 0L),
                                confirmedItemsByTripId.getOrDefault(trip.getId(), 0L)
                        ))
                        .toList()
        );
    }
}
