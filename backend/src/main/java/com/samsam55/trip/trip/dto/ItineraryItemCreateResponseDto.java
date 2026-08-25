package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.ItineraryItem;
import java.time.LocalDateTime;
import java.util.List;

public record ItineraryItemCreateResponseDto(
        Long id,
        Long tripDayId,
        String name,
        String category,
        String decisionType,
        String status,
        Integer sortOrder,
        List<VoteOptionSummaryDto> voteOptions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ItineraryItemCreateResponseDto from(ItineraryItem itineraryItem, List<VoteOptionSummaryDto> voteOptions) {
        return new ItineraryItemCreateResponseDto(
                itineraryItem.getId(),
                itineraryItem.getTripDay().getId(),
                itineraryItem.getName(),
                itineraryItem.getCategory(),
                itineraryItem.getDecisionType().name(),
                itineraryItem.getStatus().name(),
                itineraryItem.getSortOrder(),
                voteOptions,
                itineraryItem.getCreatedAt(),
                itineraryItem.getUpdatedAt()
        );
    }
}
