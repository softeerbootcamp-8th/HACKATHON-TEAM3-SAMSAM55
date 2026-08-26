package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.ItineraryItem;

public record TripItineraryItemResponseDto(
        Long id,
        String name,
        String category,
        String status,
        String decisionType,
        int optionCount
) {

    public static TripItineraryItemResponseDto from(ItineraryItem itineraryItem, int optionCount) {
        return new TripItineraryItemResponseDto(
                itineraryItem.getId(),
                itineraryItem.getName(),
                itineraryItem.getCategory(),
                itineraryItem.getStatus().name(),
                itineraryItem.getDecisionType().name(),
                optionCount
        );
    }
}
