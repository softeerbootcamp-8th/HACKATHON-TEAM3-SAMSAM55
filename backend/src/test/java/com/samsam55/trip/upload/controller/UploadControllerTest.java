package com.samsam55.trip.upload.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.samsam55.trip.auth.argumentresolver.LoginUserArgumentResolver;
import com.samsam55.trip.auth.service.AuthService;
import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.global.exception.GlobalExceptionHandler;
import com.samsam55.trip.upload.dto.PresignedUrlResponseDto;
import com.samsam55.trip.upload.exception.UploadErrorType;
import com.samsam55.trip.upload.service.S3PresignService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UploadControllerTest {

    @Mock
    private S3PresignService s3PresignService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UploadController(s3PresignService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new LoginUserArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("presigned URL 발급 요청은 200과 공통 응답 형식으로 반환한다")
    void presigned_URL_발급_요청은_200과_공통_응답_형식으로_반환한다() throws Exception {
        when(s3PresignService.issueUploadUrl("sushi.jpg")).thenReturn(new PresignedUrlResponseDto(
                "https://samsam55-trip-images.s3.ap-northeast-2.amazonaws.com/uploads/vote-options/a.jpg?X-Amz-Signature=...",
                "uploads/vote-options/a.jpg",
                "https://samsam55-trip-images.s3.ap-northeast-2.amazonaws.com/uploads/vote-options/a.jpg"));

        mockMvc.perform(get("/api/uploads/presigned-url")
                        .param("fileName", "sushi.jpg")
                        .sessionAttr(AuthService.LOGIN_USER_ID_SESSION_ATTRIBUTE, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.key").value("uploads/vote-options/a.jpg"))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    @DisplayName("로그인하지 않은 요청은 401 공통 에러 응답으로 반환한다")
    void 로그인하지_않은_요청은_401_공통_에러_응답으로_반환한다() throws Exception {
        mockMvc.perform(get("/api/uploads/presigned-url").param("fileName", "sushi.jpg"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("지원하지 않는 확장자면 415 공통 에러 응답으로 반환한다")
    void 지원하지_않는_확장자면_415_공통_에러_응답으로_반환한다() throws Exception {
        when(s3PresignService.issueUploadUrl("photo.heic"))
                .thenThrow(new ApplicationException(UploadErrorType.UNSUPPORTED_FILE_TYPE));

        mockMvc.perform(get("/api/uploads/presigned-url")
                        .param("fileName", "photo.heic")
                        .sessionAttr(AuthService.LOGIN_USER_ID_SESSION_ATTRIBUTE, 1L))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_FILE_TYPE"));
    }
}
