package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.ItineraryItem;
import com.samsam55.trip.trip.entity.TripDay;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record TripDayResponseDto(
        Long id,
        Integer dayNumber,
        LocalDate date,
        List<TripItineraryItemResponseDto> items
) {

    public static TripDayResponseDto from(
            TripDay tripDay, List<ItineraryItem> itineraryItems, Map<Long, Integer> optionCountsByItemId) {
        return new TripDayResponseDto(
                tripDay.getId(),
                tripDay.getDayNumber(),
                tripDay.getTripDate(),
                itineraryItems.stream()
                        .map(item -> TripItineraryItemResponseDto.from(
                                item, optionCountsByItemId.getOrDefault(item.getId(), 0)))
                        .toList()
        );
    }
}
