package com.samsam55.trip.trip.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.samsam55.trip.auth.argumentresolver.LoginUserArgumentResolver;
import com.samsam55.trip.auth.service.AuthService;
import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.global.exception.GlobalExceptionHandler;
import com.samsam55.trip.trip.dto.VoteOptionImageDto;
import com.samsam55.trip.trip.exception.TripErrorType;
import com.samsam55.trip.trip.service.VoteOptionService;
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

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new VoteOptionController(voteOptionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new LoginUserArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("선택지 이미지 요청은 이미지 바이트를 그대로 반환한다")
    void 선택지_이미지_요청은_이미지_바이트를_그대로_반환한다() throws Exception {
        byte[] bytes = "image-bytes".getBytes(StandardCharsets.UTF_8);
        when(voteOptionService.getImage(1L)).thenReturn(new VoteOptionImageDto(bytes, "image/jpeg"));

        mockMvc.perform(get("/api/vote-options/1/image")
                        .sessionAttr(AuthService.LOGIN_USER_ID_SESSION_ATTRIBUTE, 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes(bytes));
    }

    @Test
    @DisplayName("이미지가 없는 선택지는 404 공통 에러 응답으로 반환한다")
    void 이미지가_없는_선택지는_404_공통_에러_응답으로_반환한다() throws Exception {
        when(voteOptionService.getImage(eq(1L)))
                .thenThrow(new ApplicationException(TripErrorType.VOTE_OPTION_IMAGE_NOT_FOUND));

        mockMvc.perform(get("/api/vote-options/1/image")
                        .sessionAttr(AuthService.LOGIN_USER_ID_SESSION_ATTRIBUTE, 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("로그인하지 않은 요청은 401로 반환한다")
    void 로그인하지_않은_요청은_401로_반환한다() throws Exception {
        mockMvc.perform(get("/api/vote-options/1/image"))
                .andExpect(status().isUnauthorized());
    }
}
