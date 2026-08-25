package com.samsam55.trip.auth.service;

import com.samsam55.trip.auth.dto.AuthLoginRequestDto;
import com.samsam55.trip.auth.dto.AuthLoginResponseDto;
import com.samsam55.trip.auth.dto.AuthSignupRequestDto;
import com.samsam55.trip.auth.dto.AuthSignupResponseDto;
import com.samsam55.trip.auth.exception.AuthErrorType;
import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.member.entity.User;
import com.samsam55.trip.member.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String LOGIN_USER_ID_SESSION_ATTRIBUTE = "LOGIN_USER_ID";

    private final UserRepository userRepository;
    private final BCryptPasswordHasher passwordHasher;

    /**
     * 회원가입 정보를 검증하고 BCrypt 해시 비밀번호를 저장한다.
     *
     * @param request 회원가입 아이디와 비밀번호
     * @return 생성된 회원의 식별자와 아이디
     * @throws ApplicationException 아이디가 이미 존재할 때(DUPLICATE_LOGIN_ID)
     */
    @Transactional
    public AuthSignupResponseDto signup(AuthSignupRequestDto request) {
        if (userRepository.existsByLoginId(request.loginId())) {
            throw new ApplicationException(AuthErrorType.DUPLICATE_LOGIN_ID);
        }

        String passwordHash = passwordHasher.hash(request.password());
        try {
            User user = userRepository.saveAndFlush(new User(request.loginId(), passwordHash));
            return AuthSignupResponseDto.from(user);
        } catch (DataIntegrityViolationException e) {
            throw new ApplicationException(AuthErrorType.DUPLICATE_LOGIN_ID);
        }
    }

    /**
     * 로그인 정보를 확인하고 현재 요청에 세션을 생성한다.
     *
     * @param request 로그인 아이디와 비밀번호
     * @param servletRequest 현재 HTTP 요청
     * @return 로그인한 회원의 식별자와 아이디
     * @throws ApplicationException 아이디가 없거나 비밀번호가 틀릴 때(INVALID_CREDENTIALS)
     */
    @Transactional(readOnly = true)
    public AuthLoginResponseDto login(AuthLoginRequestDto request, HttpServletRequest servletRequest) {
        User user = userRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new ApplicationException(AuthErrorType.INVALID_CREDENTIALS));

        if (!passwordHasher.matches(request.password(), user.getPasswordHash())) {
            throw new ApplicationException(AuthErrorType.INVALID_CREDENTIALS);
        }

        HttpSession session = servletRequest.getSession(true);
        servletRequest.changeSessionId();
        session.setAttribute(LOGIN_USER_ID_SESSION_ATTRIBUTE, user.getId());
        return AuthLoginResponseDto.from(user);
    }

    /**
     * 현재 요청의 세션을 무효화한다.
     *
     * @param servletRequest 현재 HTTP 요청
     */
    public void logout(HttpServletRequest servletRequest) {
        HttpSession session = servletRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
