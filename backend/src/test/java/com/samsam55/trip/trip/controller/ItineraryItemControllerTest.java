package com.samsam55.trip.trip.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.samsam55.trip.auth.argumentresolver.LoginUserArgumentResolver;
import com.samsam55.trip.auth.service.AuthService;
import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.global.exception.GlobalExceptionHandler;
import com.samsam55.trip.trip.dto.ItineraryItemCreateRequestDto;
import com.samsam55.trip.trip.dto.ItineraryItemCreateResponseDto;
import com.samsam55.trip.trip.exception.TripErrorType;
import com.samsam55.trip.trip.service.ItineraryItemService;
import jakarta.validation.Validation;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

@ExtendWith(MockitoExtension.class)
class ItineraryItemControllerTest {

    @Mock
    private ItineraryItemService itineraryItemService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ItineraryItemController(itineraryItemService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new LoginUserArgumentResolver())
                .setValidator(new SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().getValidator()))
                .build();
    }

    private MockMultipartFile requestPart(String json) {
        return new MockMultipartFile("request", "", "application/json", json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("일정 항목 생성 요청은 201과 공통 응답 형식으로 반환한다")
    void 일정_항목_생성_요청은_201과_공통_응답_형식으로_반환한다() throws Exception {
        when(itineraryItemService.createItineraryItem(anyLong(), anyLong(), any(ItineraryItemCreateRequestDto.class), any()))
                .thenReturn(new ItineraryItemCreateResponseDto(
                        104L, 12L, "점심 메뉴", "식사", "VOTE", "PENDING", 4, List.of(),
                        LocalDateTime.now(), LocalDateTime.now()));

        MockMultipartFile image = new MockMultipartFile(
                "optionImages", "sushi.jpg", "image/jpeg", "image-bytes".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/trip-days/12/itinerary-items")
                        .file(requestPart("""
                                {"name":"점심 메뉴","category":"식사","decisionType":"VOTE","options":["스시","라멘"]}
                                """))
                        .file(image)
                        .sessionAttr(AuthService.LOGIN_USER_ID_SESSION_ATTRIBUTE, 1L))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(104))
                .andExpect(jsonPath("$.data.tripDayId").value(12))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    @DisplayName("로그인하지 않은 요청은 401 공통 에러 응답으로 반환한다")
    void 로그인하지_않은_요청은_401_공통_에러_응답으로_반환한다() throws Exception {
        mockMvc.perform(multipart("/api/trip-days/12/itinerary-items")
                        .file(requestPart("""
                                {"name":"점심 메뉴","category":"식사","decisionType":"VOTE","options":[]}
                                """)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("빈 이름으로 요청하면 400 공통 에러 응답으로 반환한다")
    void 빈_이름으로_요청하면_400_공통_에러_응답으로_반환한다() throws Exception {
        mockMvc.perform(multipart("/api/trip-days/12/itinerary-items")
                        .file(requestPart("""
                                {"name":"","category":"식사","decisionType":"VOTE","options":[]}
                                """))
                        .sessionAttr(AuthService.LOGIN_USER_ID_SESSION_ATTRIBUTE, 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @DisplayName("일차를 찾을 수 없으면 404 공통 에러 응답으로 반환한다")
    void 일차를_찾을_수_없으면_404_공통_에러_응답으로_반환한다() throws Exception {
        when(itineraryItemService.createItineraryItem(anyLong(), anyLong(), any(ItineraryItemCreateRequestDto.class), any()))
                .thenThrow(new ApplicationException(TripErrorType.TRIP_DAY_NOT_FOUND));

        mockMvc.perform(multipart("/api/trip-days/12/itinerary-items")
                        .file(requestPart("""
                                {"name":"점심 메뉴","category":"식사","decisionType":"VOTE","options":[]}
                                """))
                        .sessionAttr(AuthService.LOGIN_USER_ID_SESSION_ATTRIBUTE, 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TRIP_DAY_NOT_FOUND"));
    }
}
