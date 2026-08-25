package com.samsam55.trip.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AuthLoginRequestDto(
        @NotBlank(message = "아이디는 필수입니다.")
        String loginId,
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Pattern(regexp = "^[\\x21-\\x7E]+$", message = "비밀번호는 영문, 숫자, 특수문자만 사용할 수 있습니다.")
        @Size(max = 72, message = "비밀번호는 72자 이하여야 합니다.")
        String password
) {
}
