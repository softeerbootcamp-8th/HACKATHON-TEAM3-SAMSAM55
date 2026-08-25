package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.ItineraryItem;

public record ItineraryItemStatusDto(
        Long itemId,
        String status
) {

    public static ItineraryItemStatusDto from(ItineraryItem itineraryItem) {
        return new ItineraryItemStatusDto(itineraryItem.getId(), itineraryItem.getStatus().name());
    }
}
