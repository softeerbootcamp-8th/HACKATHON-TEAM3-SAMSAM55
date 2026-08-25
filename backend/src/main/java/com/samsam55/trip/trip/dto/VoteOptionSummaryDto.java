package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.VoteOption;

public record VoteOptionSummaryDto(
        Long id,
        String name,
        String description,
        String descriptionSource,
        boolean hasImage
) {

    public static VoteOptionSummaryDto from(VoteOption voteOption) {
        return new VoteOptionSummaryDto(
                voteOption.getId(),
                voteOption.getName(),
                voteOption.getDescription(),
                voteOption.getDescriptionSource(),
                voteOption.getImage() != null
        );
    }
}
