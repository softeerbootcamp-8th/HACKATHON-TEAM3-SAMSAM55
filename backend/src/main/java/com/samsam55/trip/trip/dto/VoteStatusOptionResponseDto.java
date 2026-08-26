package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.VoteOption;
import java.util.List;

public record VoteStatusOptionResponseDto(
        Long optionId,
        int voteCount,
        List<VoteResultParticipantResponseDto> voters
) {

    public static VoteStatusOptionResponseDto of(VoteOption option, List<VoteResultParticipantResponseDto> voters) {
        return new VoteStatusOptionResponseDto(option.getId(), voters.size(), voters);
    }
}
