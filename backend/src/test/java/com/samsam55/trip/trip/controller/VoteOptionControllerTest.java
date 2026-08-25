package com.samsam55.trip.trip.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.samsam55.trip.auth.argumentresolver.LoginUserArgumentResolver;
import com.samsam55.trip.auth.dto.AuthMeResponseDto;
import com.samsam55.trip.auth.service.AuthService;
import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.global.exception.GlobalExceptionHandler;
import com.samsam55.trip.trip.dto.VoteOptionImageDto;
import com.samsam55.trip.trip.exception.TripErrorType;
import com.samsam55.trip.trip.service.VoteOptionService;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class VoteOptionControllerTest {

    @Mock
    private VoteOptionService voteOptionService;

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new VoteOptionController(voteOptionService, authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new LoginUserArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("선택지 이미지 요청은 이미지 바이트를 그대로 반환한다")
    void 선택지_이미지_요청은_이미지_바이트를_그대로_반환한다() throws Exception {
        byte[] bytes = "image-bytes".getBytes(StandardCharsets.UTF_8);
        AuthMeResponseDto actor = AuthMeResponseDto.ofHost(1L);
        when(authService.me(any(HttpServletRequest.class))).thenReturn(actor);
        when(voteOptionService.getImage(eq(actor), eq(1L)))
                .thenReturn(new VoteOptionImageDto(bytes, "image/jpeg"));

        mockMvc.perform(get("/api/vote-options/1/image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes(bytes));

        verify(voteOptionService).getImage(actor, 1L);
    }

    @Test
    @DisplayName("이미지가 없는 선택지는 404 공통 에러 응답으로 반환한다")
    void 이미지가_없는_선택지는_404_공통_에러_응답으로_반환한다() throws Exception {
        AuthMeResponseDto actor = AuthMeResponseDto.ofHost(1L);
        when(authService.me(any(HttpServletRequest.class))).thenReturn(actor);
        when(voteOptionService.getImage(eq(actor), eq(1L)))
                .thenThrow(new ApplicationException(TripErrorType.VOTE_OPTION_IMAGE_NOT_FOUND));

        mockMvc.perform(get("/api/vote-options/1/image"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("인증하지 않은 요청은 401로 반환한다")
    void 인증하지_않은_요청은_401로_반환한다() throws Exception {
        when(authService.me(any(HttpServletRequest.class)))
                .thenThrow(new ApplicationException(com.samsam55.trip.auth.exception.AuthErrorType.UNAUTHENTICATED));

        mockMvc.perform(get("/api/vote-options/1/image"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("선택지 삭제 요청은 200과 공통 응답 형식으로 반환한다")
    void 선택지_삭제_요청은_200과_공통_응답_형식으로_반환한다() throws Exception {
        mockMvc.perform(delete("/api/vote-options/1")
                        .sessionAttr(AuthService.LOGIN_USER_ID_SESSION_ATTRIBUTE, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(voteOptionService).deleteVoteOption(1L, 1L);
    }

    @Test
    @DisplayName("투표가 시작된 선택지 삭제 요청은 409 공통 에러 응답으로 반환한다")
    void 투표가_시작된_선택지_삭제_요청은_409_공통_에러_응답으로_반환한다() throws Exception {
        org.mockito.Mockito.doThrow(new ApplicationException(TripErrorType.VOTE_ALREADY_STARTED))
                .when(voteOptionService).deleteVoteOption(1L, 1L);

        mockMvc.perform(delete("/api/vote-options/1")
                        .sessionAttr(AuthService.LOGIN_USER_ID_SESSION_ATTRIBUTE, 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("VOTE_ALREADY_STARTED"));
    }
}
