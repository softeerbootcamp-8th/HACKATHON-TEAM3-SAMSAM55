package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.TripDay;
import java.time.LocalDate;
import java.util.List;

public record ScheduleDayResponseDto(
        Long id,
        Integer dayNumber,
        LocalDate date,
        List<ScheduleItemResponseDto> items
) {

    public static ScheduleDayResponseDto of(TripDay tripDay, List<ScheduleItemResponseDto> items) {
        return new ScheduleDayResponseDto(
                tripDay.getId(),
                tripDay.getDayNumber(),
                tripDay.getTripDate(),
                items
        );
    }
}
