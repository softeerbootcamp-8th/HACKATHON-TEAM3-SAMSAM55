package com.samsam55.trip.trip.dto;

import java.util.List;

public record MyVoteBatchResponseDto(
        List<MyVoteResultDto> votes,
        Long nextItemId
) {
}
