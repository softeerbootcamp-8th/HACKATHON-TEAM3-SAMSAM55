package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.VoteOption;

public record VoteOptionSummaryDto(
        Long id,
        String name,
        boolean hasImage
) {

    public static VoteOptionSummaryDto from(VoteOption voteOption) {
        return new VoteOptionSummaryDto(voteOption.getId(), voteOption.getName(), voteOption.getImage() != null);
    }
}
