package com.samsam55.trip.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthLoginRequestDto(
        @NotBlank(message = "아이디는 필수입니다.")
        String loginId,
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
}
