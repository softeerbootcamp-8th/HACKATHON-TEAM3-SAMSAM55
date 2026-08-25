package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.ItineraryItem;
import java.util.List;

public record ItineraryItemDetailResponseDto(
        Long id,
        String name,
        String category,
        Integer dayNumber,
        String decisionType,
        String status,
        List<VoteOptionSummaryDto> voteOptions,
        Long confirmedOptionId
) {

    public static ItineraryItemDetailResponseDto from(ItineraryItem itineraryItem, List<VoteOptionSummaryDto> voteOptions) {
        return new ItineraryItemDetailResponseDto(
                itineraryItem.getId(),
                itineraryItem.getName(),
                itineraryItem.getCategory(),
                itineraryItem.getTripDay().getDayNumber(),
                itineraryItem.getDecisionType().name(),
                itineraryItem.getStatus().name(),
                voteOptions,
                itineraryItem.getConfirmedOption() != null ? itineraryItem.getConfirmedOption().getId() : null
        );
    }
}
