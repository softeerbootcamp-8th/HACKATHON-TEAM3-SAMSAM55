package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.VoteOption;

public record VoteOptionCreateResponseDto(
        Long id,
        Long itineraryItemId,
        String name,
        String description,
        String descriptionSource,
        boolean hasImage
) {

    public static VoteOptionCreateResponseDto from(VoteOption voteOption) {
        return new VoteOptionCreateResponseDto(
                voteOption.getId(),
                voteOption.getItineraryItem().getId(),
                voteOption.getName(),
                voteOption.getDescription(),
                voteOption.getDescriptionSource(),
                voteOption.getImage() != null
        );
    }
}
