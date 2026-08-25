package com.samsam55.trip.trip.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.samsam55.trip.auth.argumentresolver.LoginUserArgumentResolver;
import com.samsam55.trip.auth.service.AuthService;
import com.samsam55.trip.global.exception.GlobalExceptionHandler;
import com.samsam55.trip.trip.dto.TripListResponseDto;
import com.samsam55.trip.trip.dto.TripSummaryResponseDto;
import com.samsam55.trip.trip.service.TripService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class TripControllerTest {

    @Mock
    private TripService tripService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TripController(tripService))
                .setCustomArgumentResolvers(new LoginUserArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("로그인한 사용자의 여행 목록을 200 공통 응답으로 반환한다")
    void 로그인한_사용자의_여행_목록을_200_공통_응답으로_반환한다() throws Exception {
        when(tripService.findTrips(1L)).thenReturn(new TripListResponseDto(List.of(
                new TripSummaryResponseDto(
                        1L,
                        "제주 가족 여행",
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 3),
                        2
                )
        )));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthService.LOGIN_USER_ID_SESSION_ATTRIBUTE, 1L);

        mockMvc.perform(get("/api/trips").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].id").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("제주 가족 여행"))
                .andExpect(jsonPath("$.data.items[0].startDate").value("2026-09-01"))
                .andExpect(jsonPath("$.data.items[0].endDate").value("2026-09-03"))
                .andExpect(jsonPath("$.data.items[0].companionCount").value(2))
                .andExpect(jsonPath("$.error").isEmpty());

        verify(tripService).findTrips(1L);
    }

    @Test
    @DisplayName("로그인하지 않은 여행 목록 요청은 LOGIN_REQUIRED를 반환한다")
    void 로그인하지_않은_여행_목록_요청은_LOGIN_REQUIRED를_반환한다() throws Exception {
        mockMvc.perform(get("/api/trips"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error.code").value("LOGIN_REQUIRED"));

        verifyNoInteractions(tripService);
    }
}
