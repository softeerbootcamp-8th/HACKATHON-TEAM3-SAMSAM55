package com.samsam55.trip.auth.dto;

import com.samsam55.trip.member.entity.User;

public record AuthLoginResponseDto(Long id, String loginId) {

    public static AuthLoginResponseDto from(User user) {
        return new AuthLoginResponseDto(user.getId(), user.getLoginId());
    }
}
