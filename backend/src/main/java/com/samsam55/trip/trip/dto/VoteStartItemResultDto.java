package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.ItineraryItem;

public record VoteStartItemResultDto(Long itemId, String status) {

    public static VoteStartItemResultDto from(ItineraryItem itineraryItem) {
        return new VoteStartItemResultDto(itineraryItem.getId(), itineraryItem.getStatus().name());
    }
}
