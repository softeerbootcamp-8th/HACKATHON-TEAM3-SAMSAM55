package com.samsam55.trip.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.samsam55.trip.auth.dto.AuthLoginRequestDto;
import com.samsam55.trip.auth.dto.AuthMeResponseDto;
import com.samsam55.trip.auth.dto.AuthSignupRequestDto;
import com.samsam55.trip.auth.dto.ParticipantPrincipal;
import com.samsam55.trip.auth.exception.AuthErrorType;
import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.member.entity.User;
import com.samsam55.trip.member.repository.UserRepository;
import com.samsam55.trip.trip.service.InviteService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordHasher passwordHasher;

    @Mock
    private HttpServletRequest servletRequest;

    @Mock
    private HttpServletResponse servletResponse;

    @Mock
    private HttpSession session;

    @Mock
    private ParticipantSessionResolver participantSessionResolver;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordHasher, participantSessionResolver);
    }

    @Test
    @DisplayName("DB unique 제약 위반은 중복 아이디 에러로 변환한다")
    void DB_unique_제약_위반은_중복_아이디_에러로_변환한다() {
        when(userRepository.existsByLoginId("duplicate-user")).thenReturn(false);
        when(passwordHasher.hash("password")).thenReturn("hashed-password");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate login_id"));

        assertThatThrownBy(() -> authService.signup(
                new AuthSignupRequestDto("duplicate-user", "password")
        ))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        org.assertj.core.api.Assertions.assertThat(exception.getErrorType())
                                .isEqualTo(AuthErrorType.DUPLICATE_LOGIN_ID));
    }

    @Test
    @DisplayName("로그인 성공 시 참여자 인증을 제거하고 HOST 세션을 설정한다")
    void 로그인_성공_시_참여자_인증을_제거하고_HOST_세션을_설정한다() {
        User user = new User("login-user", "hashed-password");
        when(userRepository.findByLoginId("login-user")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("password", "hashed-password")).thenReturn(true);
        when(servletRequest.getSession(true)).thenReturn(session);

        authService.login(
                new AuthLoginRequestDto("login-user", "password"),
                servletRequest,
                servletResponse
        );

        verify(servletRequest).changeSessionId();
        verify(session).removeAttribute(InviteService.PARTICIPANT_ID_SESSION_ATTRIBUTE);
        verify(session).removeAttribute(InviteService.TRIP_ID_SESSION_ATTRIBUTE);
        verify(session).setAttribute(AuthService.LOGIN_USER_ID_SESSION_ATTRIBUTE, user.getId());

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(servletResponse).addCookie(cookieCaptor.capture());
        Cookie cookie = cookieCaptor.getValue();
        assertThat(cookie.getName()).isEqualTo(InviteService.RECOVERY_COOKIE_NAME);
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getMaxAge()).isZero();
    }

    @Test
    @DisplayName("HOST 세션이 있으면 HOST 정보를 반환한다")
    void HOST_세션이_있으면_HOST_정보를_반환한다() {
        when(servletRequest.getSession(false)).thenReturn(session);
        when(session.getAttribute(AuthService.LOGIN_USER_ID_SESSION_ATTRIBUTE)).thenReturn(5L);

        AuthMeResponseDto response = authService.me(servletRequest);

        org.assertj.core.api.Assertions.assertThat(response).isEqualTo(AuthMeResponseDto.ofHost(5L));
    }

    @Test
    @DisplayName("HOST 세션이 없으면 참여자 세션을 확인해 PARTICIPANT 정보를 반환한다")
    void HOST_세션이_없으면_참여자_정보를_반환한다() {
        when(servletRequest.getSession(false)).thenReturn(null);
        when(participantSessionResolver.resolve(servletRequest))
                .thenReturn(Optional.of(new ParticipantPrincipal(12L, 1L)));

        AuthMeResponseDto response = authService.me(servletRequest);

        org.assertj.core.api.Assertions.assertThat(response)
                .isEqualTo(AuthMeResponseDto.ofParticipant(new ParticipantPrincipal(12L, 1L)));
    }

    @Test
    @DisplayName("로그아웃 시 현재 세션과 참여자 복구 쿠키를 무효화한다")
    void 로그아웃_시_현재_세션과_참여자_복구_쿠키를_무효화한다() {
        when(servletRequest.getSession(false)).thenReturn(session);

        authService.logout(servletRequest, servletResponse);

        verify(session).invalidate();
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(servletResponse).addCookie(cookieCaptor.capture());
        Cookie cookie = cookieCaptor.getValue();
        assertThat(cookie.getName()).isEqualTo(InviteService.RECOVERY_COOKIE_NAME);
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getMaxAge()).isZero();
    }

    @Test
    @DisplayName("HOST도 PARTICIPANT도 아니면 UNAUTHENTICATED 에러를 던진다")
    void HOST도_참여자도_아니면_에러를_던진다() {
        when(servletRequest.getSession(false)).thenReturn(null);
        when(participantSessionResolver.resolve(servletRequest)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.me(servletRequest))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(AuthErrorType.UNAUTHENTICATED));
    }
}
