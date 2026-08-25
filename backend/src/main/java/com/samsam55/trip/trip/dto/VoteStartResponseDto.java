package com.samsam55.trip.trip.dto;

import java.util.List;

public record VoteStartResponseDto(
        List<ItineraryItemStatusDto> items
) {

    public static VoteStartResponseDto from(List<ItineraryItemStatusDto> items) {
        return new VoteStartResponseDto(items);
    }
}
