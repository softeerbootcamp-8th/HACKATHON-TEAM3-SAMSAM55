package com.samsam55.trip.global.exception;

import com.samsam55.trip.global.common.CommonResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("ApplicationException은 ErrorType의 상태 코드와 code/message를 그대로 응답한다")
    void ApplicationException을_처리한다() {
        ApplicationException exception = new ApplicationException(GlobalErrorType.NOT_FOUND);

        ResponseEntity<CommonResponse<Void>> response = handler.handleApplicationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().data()).isNull();
        assertThat(response.getBody().error().code()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().error().message()).isEqualTo(GlobalErrorType.NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("검증 실패 시 첫 번째 필드 에러 메시지만 응답에 담는다")
    void MethodArgumentNotValidException은_첫_번째_필드_에러만_담는다() throws NoSuchMethodException {
        FieldError first = new FieldError("request", "name", "이름은 필수입니다.");
        FieldError second = new FieldError("request", "email", "이메일 형식이 아닙니다.");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(first, second));
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(dummyMethodParameter(), bindingResult);

        ResponseEntity<CommonResponse<Void>> response = handler.handleMethodArgumentNotValidException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error().code()).isEqualTo("INVALID_INPUT_VALUE");
        assertThat(response.getBody().error().message()).isEqualTo("이름은 필수입니다.");
    }

    @Test
    @DisplayName("허용되지 않은 HTTP 메서드는 405로 응답한다")
    void HttpRequestMethodNotSupportedException을_처리한다() {
        HttpRequestMethodNotSupportedException exception = new HttpRequestMethodNotSupportedException("POST");

        ResponseEntity<CommonResponse<Void>> response = handler.handleHttpRequestMethodNotSupportedException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody().error().code()).isEqualTo("METHOD_NOT_ALLOWED");
    }

    @Test
    @DisplayName("존재하지 않는 리소스 요청은 404로 응답한다")
    void NoResourceFoundException을_처리한다() {
        NoResourceFoundException exception =
                new NoResourceFoundException(HttpMethod.GET, "/no-such-path", "no-such-path");

        ResponseEntity<CommonResponse<Void>> response = handler.handleNoResourceFoundException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().error().code()).isEqualTo("NOT_FOUND");
    }

    @Test
    @DisplayName("처리하지 못한 예외는 500과 일반 에러 메시지로 응답한다")
    void 처리되지_않은_예외는_500으로_응답한다() {
        RuntimeException exception = new RuntimeException("예상치 못한 오류");

        ResponseEntity<CommonResponse<Void>> response = handler.handleException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error().code()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().error().message()).isEqualTo(GlobalErrorType.INTERNAL_SERVER_ERROR.getMessage());
    }

    // MethodArgumentNotValidException.getMessage()가 MethodParameter의 실제 Executable을 참조하므로
    // (로그 기록 시 호출된다) mock이 아닌, 실제 메서드를 가리키는 MethodParameter가 필요하다.
    private MethodParameter dummyMethodParameter() throws NoSuchMethodException {
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyTarget", String.class);
        return new MethodParameter(method, 0);
    }

    private void dummyTarget(String name) {
    }
}
