package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.ItineraryItem;

public record ItineraryItemConfirmationResponseDto(
        Long itemId,
        String status,
        Long confirmedOptionId
) {

    public static ItineraryItemConfirmationResponseDto from(ItineraryItem itineraryItem) {
        return new ItineraryItemConfirmationResponseDto(
                itineraryItem.getId(),
                itineraryItem.getStatus().name(),
                itineraryItem.getConfirmedOption() != null ? itineraryItem.getConfirmedOption().getId() : null
        );
    }
}
