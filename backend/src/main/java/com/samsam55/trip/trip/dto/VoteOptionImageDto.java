package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.VoteOption;

public record VoteOptionImageDto(byte[] data, String contentType) {

    public static VoteOptionImageDto from(VoteOption voteOption) {
        return new VoteOptionImageDto(voteOption.getImage(), voteOption.getImageContentType());
    }
}
