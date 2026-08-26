package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.ItineraryItem;
import com.samsam55.trip.trip.entity.Trip;
import com.samsam55.trip.trip.entity.TripDay;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record TripDetailResponseDto(
        Long id,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        Integer companionCount,
        String inviteCode,
        List<TripDayResponseDto> days
) {

    public static TripDetailResponseDto from(
            Trip trip,
            List<TripDay> tripDays,
            List<ItineraryItem> itineraryItems,
            Map<Long, Integer> optionCountsByItemId
    ) {
        Map<Long, List<ItineraryItem>> itemsByTripDayId = itineraryItems.stream()
                .collect(Collectors.groupingBy(
                        itineraryItem -> itineraryItem.getTripDay().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return new TripDetailResponseDto(
                trip.getId(),
                trip.getTitle(),
                trip.getStartDate().toLocalDate(),
                trip.getEndDate().toLocalDate(),
                trip.getCompanionCount(),
                trip.getInviteCode(),
                tripDays.stream()
                        .map(tripDay -> TripDayResponseDto.from(
                                tripDay,
                                itemsByTripDayId.getOrDefault(tripDay.getId(), List.of()),
                                optionCountsByItemId
                        ))
                        .toList()
        );
    }
}
