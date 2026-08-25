package com.samsam55.trip.trip.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.samsam55.trip.auth.dto.ActorPrincipal;
import com.samsam55.trip.auth.exception.AuthErrorType;
import com.samsam55.trip.auth.service.AuthService;
import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.global.exception.GlobalExceptionHandler;
import com.samsam55.trip.trip.dto.ScheduleDayResponseDto;
import com.samsam55.trip.trip.dto.ScheduleItemResponseDto;
import com.samsam55.trip.trip.dto.ScheduleResponseDto;
import com.samsam55.trip.trip.service.ScheduleService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ScheduleControllerTest {

    @Mock
    private ScheduleService scheduleService;

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ScheduleController(scheduleService, authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("방장의 일정 조회는 공통 성공 응답으로 반환하고 서비스를 위임한다")
    void 방장의_일정_조회는_공통_성공_응답으로_반환하고_서비스를_위임한다() throws Exception {
        ActorPrincipal host = ActorPrincipal.ofHost(7L);
        when(authService.resolveActor(any())).thenReturn(host);
        when(scheduleService.findSchedule(host, 1L)).thenReturn(scheduleResponse());

        mockMvc.perform(get("/api/trips/1/schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tripId").value(1))
                .andExpect(jsonPath("$.data.votingCount").value(1))
                .andExpect(jsonPath("$.data.days[0].items[0].votedCount").value(2))
                .andExpect(jsonPath("$.data.days[0].items[0].totalParticipants").value(3))
                .andExpect(jsonPath("$.error").isEmpty());

        verify(scheduleService).findSchedule(host, 1L);
    }

    @Test
    @DisplayName("인증되지 않은 일정 조회는 401 공통 에러 응답을 반환한다")
    void 인증되지_않은_일정_조회는_401_공통_에러_응답을_반환한다() throws Exception {
        when(authService.resolveActor(any()))
                .thenThrow(new ApplicationException(AuthErrorType.UNAUTHENTICATED));

        mockMvc.perform(get("/api/trips/1/schedule"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));

        verifyNoInteractions(scheduleService);
    }

    private ScheduleResponseDto scheduleResponse() {
        return new ScheduleResponseDto(
                1L,
                "제주 가족 여행",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 3),
                1,
                List.of(new ScheduleDayResponseDto(
                        10L,
                        1,
                        LocalDate.of(2026, 9, 1),
                        List.of(new ScheduleItemResponseDto(
                                100L,
                                "점심 식사",
                                "식사",
                                "VOTE",
                                "VOTING",
                                1,
                                2,
                                3,
                                null
                        ))
                ))
        );
    }
}
