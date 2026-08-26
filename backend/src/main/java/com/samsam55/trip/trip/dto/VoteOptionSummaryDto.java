package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.VoteOption;

public record VoteOptionSummaryDto(
        Long id,
        String name,
        String description,
        String descriptionSource,
        String imageUrl
) {

    public static VoteOptionSummaryDto from(VoteOption voteOption, String imageUrl) {
        return new VoteOptionSummaryDto(
                voteOption.getId(),
                voteOption.getName(),
                voteOption.getDescription(),
                voteOption.getDescriptionSource(),
                imageUrl
        );
    }
}
