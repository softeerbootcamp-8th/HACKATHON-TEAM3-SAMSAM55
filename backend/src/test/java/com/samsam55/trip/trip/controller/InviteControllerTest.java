package com.samsam55.trip.trip.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.global.exception.GlobalExceptionHandler;
import com.samsam55.trip.trip.dto.InviteJoinRequestDto;
import com.samsam55.trip.trip.dto.InviteJoinResponseDto;
import com.samsam55.trip.trip.dto.InviteParticipantDto;
import com.samsam55.trip.trip.dto.InviteVerifyResponseDto;
import com.samsam55.trip.trip.exception.TripErrorType;
import com.samsam55.trip.trip.service.InviteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Validation;
import java.util.List;
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
class InviteControllerTest {

    @Mock
    private InviteService inviteService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new InviteController(inviteService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(new SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().getValidator()))
                .build();
    }

    @Test
    @DisplayName("초대 코드 검증은 여행 정보와 참여자 슬롯 목록을 반환한다")
    void 초대_코드_검증은_여행과_참여자_슬롯_목록을_반환한다() throws Exception {
        when(inviteService.verify("valid-code")).thenReturn(new InviteVerifyResponseDto(
                1L, "제주 가족 여행", List.of(new InviteParticipantDto(12L, "외할머니", false))
        ));

        mockMvc.perform(get("/api/invites/valid-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tripId").value(1))
                .andExpect(jsonPath("$.data.title").value("제주 가족 여행"))
                .andExpect(jsonPath("$.data.participants[0].participantId").value(12))
                .andExpect(jsonPath("$.data.participants[0].roleName").value("외할머니"))
                .andExpect(jsonPath("$.data.participants[0].joined").value(false));
    }

    @Test
    @DisplayName("존재하지 않는 초대 코드는 404 공통 에러 응답으로 반환한다")
    void 존재하지_않는_초대_코드는_404로_반환한다() throws Exception {
        when(inviteService.verify("invalid-code"))
                .thenThrow(new ApplicationException(TripErrorType.INVITE_CODE_NOT_FOUND));

        mockMvc.perform(get("/api/invites/invalid-code"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVITE_CODE_NOT_FOUND"));
    }

    @Test
    @DisplayName("participantId 없이 입장을 요청하면 400으로 반환한다")
    void participantId_없이_입장을_요청하면_400으로_반환한다() throws Exception {
        mockMvc.perform(post("/api/invites/valid-code/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT_VALUE"));

        verifyNoInteractions(inviteService);
    }

    @Test
    @DisplayName("빈 슬롯 입장은 참여자 정보를 공통 성공 응답으로 반환한다")
    void 빈_슬롯_입장은_참여자_정보를_반환한다() throws Exception {
        when(inviteService.join(
                anyString(), any(InviteJoinRequestDto.class), any(HttpServletRequest.class), any(HttpServletResponse.class)
        )).thenReturn(new InviteJoinResponseDto(12L, 1L, "외할머니"));

        mockMvc.perform(post("/api/invites/valid-code/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participantId\":12}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.participantId").value(12))
                .andExpect(jsonPath("$.data.tripId").value(1))
                .andExpect(jsonPath("$.data.roleName").value("외할머니"));
    }

    @Test
    @DisplayName("이미 선점된 슬롯으로 입장하면 409 공통 에러 응답으로 반환한다")
    void 이미_선점된_슬롯으로_입장하면_409로_반환한다() throws Exception {
        when(inviteService.join(
                anyString(), any(InviteJoinRequestDto.class), any(HttpServletRequest.class), any(HttpServletResponse.class)
        )).thenThrow(new ApplicationException(TripErrorType.PARTICIPANT_ALREADY_JOINED));

        mockMvc.perform(post("/api/invites/valid-code/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participantId\":12}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PARTICIPANT_ALREADY_JOINED"));
    }
}
