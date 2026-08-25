package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.Participant;
import com.samsam55.trip.trip.entity.VoteOption;
import java.util.List;

public record VoteResultOptionResponseDto(
        Long optionId,
        String name,
        String description,
        boolean hasImage,
        int voteCount,
        boolean isConfirmed,
        List<VoteResultParticipantResponseDto> voters
) {

    public static VoteResultOptionResponseDto of(
            VoteOption option,
            List<Participant> voters,
            boolean isConfirmed
    ) {
        return new VoteResultOptionResponseDto(
                option.getId(),
                option.getName(),
                option.getDescription(),
                option.getImage() != null,
                voters.size(),
                isConfirmed,
                voters.stream()
                        .map(VoteResultParticipantResponseDto::from)
                        .toList()
        );
    }
}
