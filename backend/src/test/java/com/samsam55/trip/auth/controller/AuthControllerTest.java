package com.samsam55.trip.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.samsam55.trip.auth.dto.AuthLoginRequestDto;
import com.samsam55.trip.auth.dto.AuthLoginResponseDto;
import com.samsam55.trip.auth.dto.AuthMeResponseDto;
import com.samsam55.trip.auth.dto.AuthSignupRequestDto;
import com.samsam55.trip.auth.dto.AuthSignupResponseDto;
import com.samsam55.trip.auth.dto.ParticipantPrincipal;
import com.samsam55.trip.auth.service.AuthService;
import com.samsam55.trip.global.exception.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(new SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().getValidator()))
                .build();
    }

    @Test
    @DisplayName("회원가입 요청은 201과 공통 응답 형식으로 반환한다")
    void 회원가입_요청은_201과_공통_응답_형식으로_반환한다() throws Exception {
        when(authService.signup(any(AuthSignupRequestDto.class)))
                .thenReturn(new AuthSignupResponseDto(1L, "signup-user"));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"signup-user\",\"password\":\"password\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.loginId").value("signup-user"))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    @DisplayName("빈 로그인 요청은 400 공통 에러 응답으로 반환한다")
    void 빈_로그인_요청은_400_공통_에러_응답으로_반환한다() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @DisplayName("72자를 초과한 회원가입 비밀번호는 400으로 반환한다")
    void 비밀번호_72자를_초과한_회원가입_요청은_400으로_반환한다() throws Exception {
        String password = "a".repeat(73);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"signup-user\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.error.message")
                        .value("비밀번호는 72자 이하여야 합니다."));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("72자를 초과한 로그인 비밀번호는 400으로 반환한다")
    void 비밀번호_72자를_초과한_로그인_요청은_400으로_반환한다() throws Exception {
        String password = "a".repeat(73);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"login-user\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.error.message")
                        .value("비밀번호는 72자 이하여야 합니다."));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("허용하지 않는 문자가 포함된 로그인 비밀번호는 400으로 반환한다")
    void 허용하지_않는_문자가_포함된_로그인_비밀번호는_400으로_반환한다() throws Exception {
        String password = "비밀번호123";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"login-user\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.error.message")
                        .value("비밀번호는 영문, 숫자, 특수문자만 사용할 수 있습니다."));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("로그인은 세션이 포함된 공통 성공 응답으로 반환한다")
    void 로그인은_세션이_포함된_공통_성공_응답으로_반환한다() throws Exception {
        when(authService.login(any(AuthLoginRequestDto.class), any(HttpServletRequest.class)))
                .thenReturn(new AuthLoginResponseDto(1L, "login-user"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"login-user\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loginId").value("login-user"))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    @DisplayName("로그아웃은 현재 요청을 서비스에 전달하고 공통 성공 응답을 반환한다")
    void 로그아웃은_현재_요청을_서비스에_전달하고_공통_성공_응답을_반환한다() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error").isEmpty());

        verify(authService).logout(any(HttpServletRequest.class));
    }

    @Test
    @DisplayName("현재 접근자 조회는 HOST 정보를 공통 성공 응답으로 반환한다")
    void 현재_접근자_조회는_HOST_정보를_반환한다() throws Exception {
        when(authService.me(any(HttpServletRequest.class))).thenReturn(AuthMeResponseDto.ofHost(1L));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.actorType").value("HOST"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    @DisplayName("현재 접근자 조회는 PARTICIPANT 정보를 공통 성공 응답으로 반환한다")
    void 현재_접근자_조회는_PARTICIPANT_정보를_반환한다() throws Exception {
        when(authService.me(any(HttpServletRequest.class)))
                .thenReturn(AuthMeResponseDto.ofParticipant(new ParticipantPrincipal(12L, 1L)));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actorType").value("PARTICIPANT"))
                .andExpect(jsonPath("$.data.participantId").value(12))
                .andExpect(jsonPath("$.data.tripId").value(1));
    }
}
