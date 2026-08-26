package com.samsam55.trip.trip.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.samsam55.trip.auth.argumentresolver.LoginUserArgumentResolver;
import com.samsam55.trip.auth.service.AuthService;
import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.global.exception.GlobalErrorType;
import com.samsam55.trip.global.exception.GlobalExceptionHandler;
import com.samsam55.trip.trip.dto.VoteOptionSummaryDto;
import com.samsam55.trip.trip.exception.TripErrorType;
import com.samsam55.trip.trip.service.VoteOptionService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

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
                .setValidator(new SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().getValidator()))
                .build();
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
        Mockito.doThrow(new ApplicationException(TripErrorType.VOTE_ALREADY_STARTED))
                .when(voteOptionService).deleteVoteOption(1L, 1L);

        mockMvc.perform(delete("/api/vote-options/1")
                        .sessionAttr(AuthService.LOGIN_USER_ID_SESSION_ATTRIBUTE, 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("VOTE_ALREADY_STARTED"));
    }

    @Test
    @DisplayName("선택지 수정 요청은 200과 공통 응답 형식으로 반환한다")
    void 선택지_수정_요청은_200과_공통_응답_형식으로_반환한다() throws Exception {
        when(voteOptionService.updateVoteOption(1L, 1L, "라멘", "설명", null))
                .thenReturn(new VoteOptionSummaryDto(1L, "라멘", "설명", "HOST", null));

        mockMvc.perform(put("/api/vote-options/1")
                        .contentType("application/json")
                        .content("""
                                {"name":"라멘","description":"설명","imageKey":null}
                                """)
                        .sessionAttr(AuthService.LOGIN_USER_ID_SESSION_ATTRIBUTE, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("라멘"))
                .andExpect(jsonPath("$.data.descriptionSource").value("HOST"));
    }

    @Test
    @DisplayName("이름 없이 선택지 수정 요청을 보내면 400 공통 에러 응답으로 반환한다")
    void 이름_없이_선택지_수정_요청을_보내면_400_공통_에러_응답으로_반환한다() throws Exception {
        mockMvc.perform(put("/api/vote-options/1")
                        .contentType("application/json")
                        .content("""
                                {"name":"  ","description":null,"imageKey":null}
                                """)
                        .sessionAttr(AuthService.LOGIN_USER_ID_SESSION_ATTRIBUTE, 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @DisplayName("투표가 시작된 선택지 수정 요청은 409 공통 에러 응답으로 반환한다")
    void 투표가_시작된_선택지_수정_요청은_409_공통_에러_응답으로_반환한다() throws Exception {
        when(voteOptionService.updateVoteOption(1L, 1L, "라멘", "설명", null))
                .thenThrow(new ApplicationException(TripErrorType.VOTE_ALREADY_STARTED));

        mockMvc.perform(put("/api/vote-options/1")
                        .contentType("application/json")
                        .content("""
                                {"name":"라멘","description":"설명","imageKey":null}
                                """)
                        .sessionAttr(AuthService.LOGIN_USER_ID_SESSION_ATTRIBUTE, 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("VOTE_ALREADY_STARTED"));
    }
}
