package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.Trip;
import java.time.LocalDate;
import java.util.List;

public record ScheduleResponseDto(
        Long tripId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        int votingCount,
        List<ScheduleDayResponseDto> days
) {

    public static ScheduleResponseDto of(
            Trip trip,
            int votingCount,
            List<ScheduleDayResponseDto> days
    ) {
        return new ScheduleResponseDto(
                trip.getId(),
                trip.getTitle(),
                trip.getStartDate().toLocalDate(),
                trip.getEndDate().toLocalDate(),
                votingCount,
                days
        );
    }
}
