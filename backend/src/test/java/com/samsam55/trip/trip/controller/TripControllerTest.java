package com.samsam55.trip.trip.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.samsam55.trip.auth.argumentresolver.LoginUserArgumentResolver;
import com.samsam55.trip.auth.service.AuthService;
import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.global.exception.GlobalExceptionHandler;
import com.samsam55.trip.trip.dto.TripCreateResponseDto;
import com.samsam55.trip.trip.dto.TripDayResponseDto;
import com.samsam55.trip.trip.dto.TripDetailResponseDto;
import com.samsam55.trip.trip.dto.TripListResponseDto;
import com.samsam55.trip.trip.dto.TripParticipantResponseDto;
import com.samsam55.trip.trip.dto.TripSummaryResponseDto;
import com.samsam55.trip.trip.dto.TripItineraryItemResponseDto;
import com.samsam55.trip.trip.exception.TripErrorType;
import com.samsam55.trip.trip.service.TripService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
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
                        2,
                        8L,
                        5L,
                        62
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
                .andExpect(jsonPath("$.data.items[0].totalItems").value(8))
                .andExpect(jsonPath("$.data.items[0].confirmedItems").value(5))
                .andExpect(jsonPath("$.data.items[0].progressPercent").value(62))
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

    @Test
    @DisplayName("로그인한 사용자의 여행 생성을 201 공통 응답으로 반환한다")
    void 로그인한_사용자의_여행_생성을_201_공통_응답으로_반환한다() throws Exception {
        when(tripService.createTrip(eq(1L), any())).thenReturn(new TripCreateResponseDto(
                1L,
                "제주 가족 여행",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 3),
                2,
                "abc123",
                List.of(
                        new TripParticipantResponseDto(10L, "엄마"),
                        new TripParticipantResponseDto(11L, "아빠")
                )
        ));
        MockHttpSession session = loginSession();

        mockMvc.perform(post("/api/trips")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "제주 가족 여행",
                                  "startDate": "2026-09-01",
                                  "endDate": "2026-09-03",
                                  "companions": ["엄마", "아빠"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.startDate").value("2026-09-01"))
                .andExpect(jsonPath("$.data.endDate").value("2026-09-03"))
                .andExpect(jsonPath("$.data.companionCount").value(2))
                .andExpect(jsonPath("$.data.inviteCode").value("abc123"))
                .andExpect(jsonPath("$.data.participants[0].participantId").value(10))
                .andExpect(jsonPath("$.data.participants[0].roleName").value("엄마"))
                .andExpect(jsonPath("$.error").isEmpty());

        verify(tripService).createTrip(eq(1L), any());
    }

    @Test
    @DisplayName("로그인하지 않은 여행 생성 요청은 LOGIN_REQUIRED를 반환한다")
    void 로그인하지_않은_여행_생성_요청은_LOGIN_REQUIRED를_반환한다() throws Exception {
        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("LOGIN_REQUIRED"));

        verifyNoInteractions(tripService);
    }

    @Test
    @DisplayName("여행 기간이 잘못되면 INVALID_TRIP_PERIOD를 반환한다")
    void 여행_기간이_잘못되면_INVALID_TRIP_PERIOD를_반환한다() throws Exception {
        when(tripService.createTrip(eq(1L), any()))
                .thenThrow(new ApplicationException(TripErrorType.INVALID_TRIP_PERIOD));

        mockMvc.perform(post("/api/trips")
                        .session(loginSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "잘못된 여행",
                                  "startDate": "2026-09-03",
                                  "endDate": "2026-09-01",
                                  "companions": ["엄마"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error.code").value("INVALID_TRIP_PERIOD"));
    }

    @Test
    @DisplayName("필수 여행 생성 필드가 비어 있으면 400을 반환한다")
    void 필수_여행_생성_필드가_비어_있으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/trips")
                        .session(loginSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT_VALUE"));

        verifyNoInteractions(tripService);
    }

    @Test
    @DisplayName("방장의 여행 삭제는 200 공통 성공 응답을 반환한다")
    void 방장의_여행_삭제는_200_공통_성공_응답을_반환한다() throws Exception {
        mockMvc.perform(delete("/api/trips/1").session(loginSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error").isEmpty());

        verify(tripService).deleteTrip(1L, 1L);
    }

    @Test
    @DisplayName("로그인하지 않은 여행 삭제 요청은 LOGIN_REQUIRED를 반환한다")
    void 로그인하지_않은_여행_삭제_요청은_LOGIN_REQUIRED를_반환한다() throws Exception {
        mockMvc.perform(delete("/api/trips/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("LOGIN_REQUIRED"));

        verifyNoInteractions(tripService);
    }

    @Test
    @DisplayName("방장이 아닌 사용자의 여행 삭제 요청은 TRIP_NOT_FOUND를 반환한다")
    void 방장이_아닌_사용자의_여행_삭제_요청은_TRIP_NOT_FOUND를_반환한다() throws Exception {
        doThrow(new ApplicationException(TripErrorType.TRIP_NOT_FOUND))
                .when(tripService)
                .deleteTrip(1L, 1L);

        mockMvc.perform(delete("/api/trips/1").session(loginSession()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error.code").value("TRIP_NOT_FOUND"));
    }

    @Test
    @DisplayName("방장의 여행 상세 조회는 200 공통 응답으로 반환한다")
    void 방장의_여행_상세_조회는_200_공통_응답으로_반환한다() throws Exception {
        when(tripService.findTrip(1L, 1L)).thenReturn(new TripDetailResponseDto(
                1L,
                "제주 가족 여행",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 3),
                2,
                "invite-code",
                List.of(new TripDayResponseDto(
                        10L,
                        1,
                        LocalDate.of(2026, 9, 1),
                        List.of(new TripItineraryItemResponseDto(
                                100L,
                                "점심 식사",
                                "식사",
                                "VOTING"
                        ))
                ))
        ));

        mockMvc.perform(get("/api/trips/1").session(loginSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("제주 가족 여행"))
                .andExpect(jsonPath("$.data.startDate").value("2026-09-01"))
                .andExpect(jsonPath("$.data.endDate").value("2026-09-03"))
                .andExpect(jsonPath("$.data.companionCount").value(2))
                .andExpect(jsonPath("$.data.inviteCode").value("invite-code"))
                .andExpect(jsonPath("$.data.days[0].id").value(10))
                .andExpect(jsonPath("$.data.days[0].dayNumber").value(1))
                .andExpect(jsonPath("$.data.days[0].date").value("2026-09-01"))
                .andExpect(jsonPath("$.data.days[0].items[0].id").value(100))
                .andExpect(jsonPath("$.data.days[0].items[0].name").value("점심 식사"))
                .andExpect(jsonPath("$.data.days[0].items[0].category").value("식사"))
                .andExpect(jsonPath("$.data.days[0].items[0].status").value("VOTING"))
                .andExpect(jsonPath("$.error").isEmpty());

        verify(tripService).findTrip(1L, 1L);
    }

    @Test
    @DisplayName("로그인하지 않은 여행 상세 요청은 LOGIN_REQUIRED를 반환한다")
    void 로그인하지_않은_여행_상세_요청은_LOGIN_REQUIRED를_반환한다() throws Exception {
        mockMvc.perform(get("/api/trips/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("LOGIN_REQUIRED"));

        verifyNoInteractions(tripService);
    }

    @Test
    @DisplayName("방장이 아닌 사용자의 여행 상세 요청은 TRIP_NOT_FOUND를 반환한다")
    void 방장이_아닌_사용자의_여행_상세_요청은_TRIP_NOT_FOUND를_반환한다() throws Exception {
        when(tripService.findTrip(1L, 1L))
                .thenThrow(new ApplicationException(TripErrorType.TRIP_NOT_FOUND));

        mockMvc.perform(get("/api/trips/1").session(loginSession()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error.code").value("TRIP_NOT_FOUND"));
    }

    private MockHttpSession loginSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthService.LOGIN_USER_ID_SESSION_ATTRIBUTE, 1L);
        return session;
    }
}
