package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.VoteOption;

public record ScheduleConfirmedOptionResponseDto(
        Long id,
        String name,
        String description,
        boolean hasImage
) {

    public static ScheduleConfirmedOptionResponseDto from(VoteOption option) {
        if (option == null) {
            return null;
        }
        return new ScheduleConfirmedOptionResponseDto(
                option.getId(),
                option.getName(),
                option.getDescription(),
                option.getImage() != null
        );
    }
}
