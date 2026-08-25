package com.samsam55.trip.trip.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.samsam55.trip.auth.argumentresolver.LoginUserArgumentResolver;
import com.samsam55.trip.auth.argumentresolver.ParticipantArgumentResolver;
import com.samsam55.trip.auth.dto.ParticipantPrincipal;
import com.samsam55.trip.auth.service.AuthService;
import com.samsam55.trip.auth.service.ParticipantSessionResolver;
import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.global.exception.GlobalExceptionHandler;
import com.samsam55.trip.trip.dto.ItineraryItemConfirmationResponseDto;
import com.samsam55.trip.trip.dto.ItineraryItemStatusDto;
import com.samsam55.trip.trip.dto.MyVoteBatchResponseDto;
import com.samsam55.trip.trip.dto.MyVoteResultDto;
import com.samsam55.trip.trip.dto.VoteStartResponseDto;
import com.samsam55.trip.trip.exception.TripErrorType;
import com.samsam55.trip.trip.service.VoteService;
import jakarta.validation.Validation;
import java.util.List;
import java.util.Optional;
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
class VoteControllerTest {

    @Mock
    private VoteService voteService;

    @Mock
    private ParticipantSessionResolver participantSessionResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new VoteController(voteService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                        new LoginUserArgumentResolver(),
                        new ParticipantArgumentResolver(participantSessionResolver))
                .setValidator(new SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().getValidator()))
                .build();
    }

    @Test
    @DisplayName("투표 시작 요청은 200과 공통 응답 형식으로 반환한다")
    void 투표_시작_요청은_200과_공통_응답_형식으로_반환한다() throws Exception {
        when(voteService.startVote(eq(1L), anyList()))
                .thenReturn(new VoteStartResponseDto(List.of(new ItineraryItemStatusDto(101L, "VOTING"))));

        mockMvc.perform(post("/api/itinerary-items/vote/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemIds":[101]}
                                """)
                        .sessionAttr(AuthService.LOGIN_USER_ID_SESSION_ATTRIBUTE, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].itemId").value(101))
                .andExpect(jsonPath("$.data.items[0].status").value("VOTING"));
    }

    @Test
    @DisplayName("로그인하지 않은 투표 시작 요청은 401 공통 에러 응답으로 반환한다")
    void 로그인하지_않은_투표_시작_요청은_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/itinerary-items/vote/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemIds":[101]}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("itemIds가 비어 있으면 400 공통 에러 응답으로 반환한다")
    void 투표_시작_itemIds가_비어_있으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/itinerary-items/vote/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemIds":[]}
                                """)
                        .sessionAttr(AuthService.LOGIN_USER_ID_SESSION_ATTRIBUTE, 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @DisplayName("투표 시작 조건을 만족하지 못하면 409 공통 에러 응답으로 반환한다")
    void 투표_시작_조건을_만족하지_못하면_409를_반환한다() throws Exception {
        when(voteService.startVote(eq(1L), anyList()))
                .thenThrow(new ApplicationException(TripErrorType.VOTE_OPTION_COUNT_INSUFFICIENT,
                        TripErrorType.VOTE_OPTION_COUNT_INSUFFICIENT.getMessage() + " (itemId: 101)"));

        mockMvc.perform(post("/api/itinerary-items/vote/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemIds":[101]}
                                """)
                        .sessionAttr(AuthService.LOGIN_USER_ID_SESSION_ATTRIBUTE, 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("VOTE_OPTION_COUNT_INSUFFICIENT"))
                .andExpect(jsonPath("$.error.message").value(
                        TripErrorType.VOTE_OPTION_COUNT_INSUFFICIENT.getMessage() + " (itemId: 101)"));
    }

    @Test
    @DisplayName("내 투표 제출 요청은 200과 공통 응답 형식으로 반환한다")
    void 내_투표_제출_요청은_200과_공통_응답_형식으로_반환한다() throws Exception {
        ParticipantPrincipal principal = new ParticipantPrincipal(11L, 5L);
        when(participantSessionResolver.resolve(any())).thenReturn(Optional.of(principal));
        when(voteService.castVotes(eq(principal), anyList()))
                .thenReturn(new MyVoteBatchResponseDto(List.of(new MyVoteResultDto(101L, 1001L)), 105L));

        mockMvc.perform(put("/api/itinerary-items/my-votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"votes":[{"itemId":101,"voteOptionId":1001}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.votes[0].itemId").value(101))
                .andExpect(jsonPath("$.data.votes[0].voteOptionId").value(1001))
                .andExpect(jsonPath("$.data.nextItemId").value(105));
    }

    @Test
    @DisplayName("참여자 인증이 없는 내 투표 제출 요청은 401 공통 에러 응답으로 반환한다")
    void 인증이_없는_내_투표_제출_요청은_401을_반환한다() throws Exception {
        when(participantSessionResolver.resolve(any())).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/itinerary-items/my-votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"votes":[{"itemId":101,"voteOptionId":1001}]}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("PARTICIPANT_LOGIN_REQUIRED"));
    }

    @Test
    @DisplayName("일정 항목 확정 요청은 200과 공통 응답 형식으로 반환한다")
    void 일정_항목_확정_요청은_200과_공통_응답_형식으로_반환한다() throws Exception {
        when(voteService.confirm(eq(1L), eq(101L), eq(1001L)))
                .thenReturn(new ItineraryItemConfirmationResponseDto(101L, "CONFIRMED", 1001L));

        mockMvc.perform(put("/api/itinerary-items/101/confirmation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"voteOptionId":1001}
                                """)
                        .sessionAttr(AuthService.LOGIN_USER_ID_SESSION_ATTRIBUTE, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemId").value(101))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.confirmedOptionId").value(1001));
    }

    @Test
    @DisplayName("일정 확정 해제 요청은 200과 공통 응답 형식으로 반환한다")
    void 일정_확정_해제_요청은_200과_공통_응답_형식으로_반환한다() throws Exception {
        when(voteService.unconfirm(1L, 101L)).thenReturn(new ItineraryItemStatusDto(101L, "VOTING"));

        mockMvc.perform(delete("/api/itinerary-items/101/confirmation")
                        .sessionAttr(AuthService.LOGIN_USER_ID_SESSION_ATTRIBUTE, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemId").value(101))
                .andExpect(jsonPath("$.data.status").value("VOTING"));
    }

    @Test
    @DisplayName("확정된 일정이 아닌데 해제를 요청하면 409 공통 에러 응답으로 반환한다")
    void 확정_상태가_아닌데_해제를_요청하면_409를_반환한다() throws Exception {
        when(voteService.unconfirm(1L, 101L))
                .thenThrow(new ApplicationException(TripErrorType.ITINERARY_ITEM_NOT_CONFIRMED));

        mockMvc.perform(delete("/api/itinerary-items/101/confirmation")
                        .sessionAttr(AuthService.LOGIN_USER_ID_SESSION_ATTRIBUTE, 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ITINERARY_ITEM_NOT_CONFIRMED"));
    }

    @Test
    @DisplayName("일정 항목을 찾을 수 없으면 404 공통 에러 응답으로 반환한다")
    void 일정_항목을_찾을_수_없으면_404를_반환한다() throws Exception {
        when(voteService.confirm(anyLong(), eq(999L), eq(1001L)))
                .thenThrow(new ApplicationException(TripErrorType.ITINERARY_ITEM_NOT_FOUND));

        mockMvc.perform(put("/api/itinerary-items/999/confirmation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"voteOptionId":1001}
                                """)
                        .sessionAttr(AuthService.LOGIN_USER_ID_SESSION_ATTRIBUTE, 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ITINERARY_ITEM_NOT_FOUND"));
    }
}
