package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.ItineraryItem;
import com.samsam55.trip.trip.entity.TripDay;
import java.time.LocalDate;
import java.util.List;

public record TripDayResponseDto(
        Long id,
        Integer dayNumber,
        LocalDate date,
        List<TripItineraryItemResponseDto> items
) {

    public static TripDayResponseDto from(TripDay tripDay, List<ItineraryItem> itineraryItems) {
        return new TripDayResponseDto(
                tripDay.getId(),
                tripDay.getDayNumber(),
                tripDay.getTripDate(),
                itineraryItems.stream()
                        .map(TripItineraryItemResponseDto::from)
                        .toList()
        );
    }
}
