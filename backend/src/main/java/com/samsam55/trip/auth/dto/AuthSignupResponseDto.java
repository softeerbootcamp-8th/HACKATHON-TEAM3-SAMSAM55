package com.samsam55.trip.auth.dto;

import com.samsam55.trip.member.entity.User;

public record AuthSignupResponseDto(Long id, String loginId) {

    public static AuthSignupResponseDto from(User user) {
        return new AuthSignupResponseDto(user.getId(), user.getLoginId());
    }
}
