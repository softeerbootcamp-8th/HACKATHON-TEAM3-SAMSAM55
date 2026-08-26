package com.samsam55.trip.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.samsam55.trip.auth.dto.AuthLoginRequestDto;
import com.samsam55.trip.auth.dto.AuthLoginResponseDto;
import com.samsam55.trip.auth.dto.AuthSignupRequestDto;
import com.samsam55.trip.auth.dto.AuthSignupResponseDto;
import com.samsam55.trip.auth.exception.AuthErrorType;
import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.global.support.AbstractMySqlContainerTest;
import com.samsam55.trip.member.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.mindrot.jbcrypt.BCrypt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AuthServiceIntegrationTest extends AbstractMySqlContainerTest {

    private final AuthService authService;
    private final UserRepository userRepository;

    @Autowired
    AuthServiceIntegrationTest(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @Test
    @DisplayName("회원가입 시 비밀번호를 BCrypt로 해시하여 저장한다")
    void 회원가입_시_비밀번호를_BCrypt로_해시하여_저장한다() {
        AuthSignupResponseDto response = authService.signup(
                new AuthSignupRequestDto("signup-user", "plain-password")
        );

        var user = userRepository.findByLoginId("signup-user").orElseThrow();

        assertThat(response.id()).isEqualTo(user.getId());
        assertThat(response.loginId()).isEqualTo("signup-user");
        assertThat(user.getPasswordHash()).isNotEqualTo("plain-password");
        assertThat(BCrypt.checkpw("plain-password", user.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("이미 사용 중인 loginId로 회원가입할 수 없다")
    void 이미_사용중인_loginId로_회원가입할_수_없다() {
        authService.signup(new AuthSignupRequestDto("duplicate-user", "password"));

        assertThatThrownBy(() -> authService.signup(
                new AuthSignupRequestDto("duplicate-user", "different-password")
        ))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(AuthErrorType.DUPLICATE_LOGIN_ID));
    }

    @Test
    @DisplayName("로그인 성공 시 회원 식별자를 세션에 저장한다")
    void 로그인_성공_시_회원_식별자를_세션에_저장한다() {
        authService.signup(new AuthSignupRequestDto("login-user", "password"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        AuthLoginResponseDto response = authService.login(
                new AuthLoginRequestDto("login-user", "password"), request, servletResponse
        );

        HttpSession session = request.getSession(false);
        assertThat(response.loginId()).isEqualTo("login-user");
        assertThat(session).isNotNull();
        assertThat(session.getAttribute(AuthService.LOGIN_USER_ID_SESSION_ATTRIBUTE))
                .isEqualTo(response.id());
    }

    @Test
    @DisplayName("로그인 실패 시 세션을 생성하지 않는다")
    void 로그인_실패_시_세션을_생성하지_않는다() {
        authService.signup(new AuthSignupRequestDto("login-failure-user", "password"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        assertThatThrownBy(() -> authService.login(
                new AuthLoginRequestDto("login-failure-user", "wrong-password"), request, servletResponse
        ))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(AuthErrorType.INVALID_CREDENTIALS));

        assertThat(request.getSession(false)).isNull();
    }

    @Test
    @DisplayName("로그아웃 시 현재 세션을 무효화한다")
    void 로그아웃_시_현재_세션을_무효화한다() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        HttpSession session = request.getSession(true);

        authService.logout(request, response);

        assertThat(((org.springframework.mock.web.MockHttpSession) session).isInvalid()).isTrue();
        assertThat(response.getCookie("PARTICIPANT_TOKEN").getMaxAge()).isZero();
    }
}
