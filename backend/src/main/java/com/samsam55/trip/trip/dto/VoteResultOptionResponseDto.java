package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.Participant;
import com.samsam55.trip.trip.entity.VoteOption;
import java.util.List;

public record VoteResultOptionResponseDto(
        Long optionId,
        String name,
        String description,
        String imageUrl,
        int voteCount,
        boolean isConfirmed,
        List<VoteResultParticipantResponseDto> voters
) {

    public static VoteResultOptionResponseDto of(
            VoteOption option,
            String imageUrl,
            List<Participant> voters,
            boolean isConfirmed
    ) {
        return new VoteResultOptionResponseDto(
                option.getId(),
                option.getName(),
                option.getDescription(),
                imageUrl,
                voters.size(),
                isConfirmed,
                voters.stream()
                        .map(VoteResultParticipantResponseDto::from)
                        .toList()
        );
    }
}
