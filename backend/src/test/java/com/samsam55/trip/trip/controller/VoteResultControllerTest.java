package com.samsam55.trip.trip.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.samsam55.trip.auth.argumentresolver.ParticipantArgumentResolver;
import com.samsam55.trip.auth.dto.ParticipantPrincipal;
import com.samsam55.trip.auth.service.ParticipantSessionResolver;
import com.samsam55.trip.global.exception.GlobalExceptionHandler;
import com.samsam55.trip.trip.dto.VoteResultOptionResponseDto;
import com.samsam55.trip.trip.dto.VoteResultParticipantResponseDto;
import com.samsam55.trip.trip.dto.VoteResultResponseDto;
import com.samsam55.trip.trip.service.ScheduleService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class VoteResultControllerTest {

    @Mock
    private ScheduleService scheduleService;

    @Mock
    private ParticipantSessionResolver participantSessionResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new VoteResultController(scheduleService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new ParticipantArgumentResolver(participantSessionResolver))
                .build();
    }

    @Test
    @DisplayName("참여자의 투표 결과 조회는 공통 성공 응답으로 반환하고 서비스를 위임한다")
    void 참여자의_투표_결과_조회는_공통_성공_응답으로_반환하고_서비스를_위임한다() throws Exception {
        ParticipantPrincipal participant = new ParticipantPrincipal(12L, 1L);
        when(participantSessionResolver.resolve(any())).thenReturn(Optional.of(participant));
        when(scheduleService.findVoteResult(participant, 100L)).thenReturn(voteResultResponse());

        mockMvc.perform(get("/api/itinerary-items/100/vote-results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.itemId").value(100))
                .andExpect(jsonPath("$.data.totalParticipants").value(3))
                .andExpect(jsonPath("$.data.votedCount").value(2))
                .andExpect(jsonPath("$.data.pendingParticipants[0].roleName").value("아빠"))
                .andExpect(jsonPath("$.data.options[0].voteCount").value(2))
                .andExpect(jsonPath("$.data.options[0].isConfirmed").value(false))
                .andExpect(jsonPath("$.data.options[0].voters[0].roleName").value("엄마"))
                .andExpect(jsonPath("$.error").isEmpty());

        verify(scheduleService).findVoteResult(participant, 100L);
    }

    @Test
    @DisplayName("인증되지 않은 투표 결과 조회는 401 공통 에러 응답을 반환한다")
    void 인증되지_않은_투표_결과_조회는_401_공통_에러_응답을_반환한다() throws Exception {
        when(participantSessionResolver.resolve(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/itinerary-items/100/vote-results"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error.code").value("PARTICIPANT_LOGIN_REQUIRED"));

        verifyNoInteractions(scheduleService);
    }

    private VoteResultResponseDto voteResultResponse() {
        VoteResultParticipantResponseDto mother = new VoteResultParticipantResponseDto(11L, "엄마");
        VoteResultParticipantResponseDto father = new VoteResultParticipantResponseDto(12L, "아빠");
        VoteResultParticipantResponseDto child = new VoteResultParticipantResponseDto(13L, "첫째");
        return new VoteResultResponseDto(
                100L,
                "점심 식사",
                "식사",
                "VOTING",
                1,
                LocalDate.of(2026, 9, 1),
                3,
                2,
                2,
                null,
                List.of(mother, father, child),
                List.of(father),
                List.of(new VoteResultOptionResponseDto(
                        201L,
                        "스시 오마카세 긴자점",
                        "신선한 제철 재료로 만든 프리미엄 스시 코스",
                        true,
                        2,
                        false,
                        List.of(mother, child)
                ))
        );
    }
}
