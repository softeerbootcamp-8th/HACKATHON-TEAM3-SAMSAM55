package com.samsam55.trip.global.exception;

import com.samsam55.trip.auth.service.ParticipantSessionResolver;
import com.samsam55.trip.global.exception.support.GlobalExceptionTestController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GlobalExceptionTestController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ParticipantArgumentResolver는 @WebMvcTest 슬라이스에 포함되지만, 의존 서비스는 슬라이스 밖이라 목으로 채워준다.
    @MockitoBean
    private ParticipantSessionResolver participantSessionResolver;

    @Test
    @DisplayName("@Valid 검증 실패 시 400과 공통 응답 형식(success/data/error)으로 변환된다")
    void 검증_실패는_공통_응답_형식의_400으로_변환된다() throws Exception {
        mockMvc.perform(post("/test/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.error.message").value("이름은 필수입니다."));
    }

    @Test
    @DisplayName("요청 본문이 올바른 JSON이 아니면 400과 공통 응답 형식으로 변환된다")
    void 요청_본문이_올바른_JSON이_아니면_400으로_변환된다() throws Exception {
        mockMvc.perform(post("/test/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{올바르지-않은-JSON}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @DisplayName("ApplicationException을 던지면 지정한 상태 코드로 응답한다")
    void ApplicationException은_지정한_상태코드로_응답한다() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }
}
