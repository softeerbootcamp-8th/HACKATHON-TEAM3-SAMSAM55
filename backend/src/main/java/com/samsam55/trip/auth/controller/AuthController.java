package com.samsam55.trip.auth.controller;

import com.samsam55.trip.auth.dto.AuthLoginRequestDto;
import com.samsam55.trip.auth.dto.AuthLoginResponseDto;
import com.samsam55.trip.auth.dto.AuthSignupRequestDto;
import com.samsam55.trip.auth.dto.AuthSignupResponseDto;
import com.samsam55.trip.auth.service.AuthService;
import com.samsam55.trip.global.common.CommonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 회원가입을 처리한다.
     *
     * @param request 회원가입 아이디와 비밀번호
     * @return 생성된 회원 정보가 담긴 201 응답
     */
    @PostMapping("/signup")
    public ResponseEntity<CommonResponse<AuthSignupResponseDto>> signup(
            @Valid @RequestBody AuthSignupRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.success(authService.signup(request)));
    }

    /**
     * 로그인을 처리하고 세션 쿠키를 발급한다.
     *
     * @param request 로그인 아이디와 비밀번호
     * @param servletRequest 세션을 생성할 현재 HTTP 요청
     * @return 로그인한 회원 정보가 담긴 200 응답
     */
    @PostMapping("/login")
    public CommonResponse<AuthLoginResponseDto> login(
            @Valid @RequestBody AuthLoginRequestDto request,
            HttpServletRequest servletRequest
    ) {
        return CommonResponse.success(authService.login(request, servletRequest));
    }

    /**
     * 현재 세션을 무효화하여 로그아웃한다.
     *
     * @param servletRequest 현재 HTTP 요청
     * @return 데이터가 없는 200 응답
     */
    @PostMapping("/logout")
    public CommonResponse<Void> logout(HttpServletRequest servletRequest) {
        authService.logout(servletRequest);
        return CommonResponse.empty();
    }
}
