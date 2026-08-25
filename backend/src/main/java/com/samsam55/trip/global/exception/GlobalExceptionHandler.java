package com.samsam55.trip.global.exception;

import com.samsam55.trip.global.common.CommonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 컨트롤러에서 발생한 예외를 공통 응답 형식({@code success}/{@code data}/{@code error})으로 변환한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 도메인에서 던진 {@link ApplicationException}을 해당 {@link ErrorType}의
     * HTTP 상태 코드와 code/message로 변환한다.
     *
     * @param e 도메인 코드에서 던진 예외
     * @return {@code error.code}/{@code error.message}가 채워진 공통 응답
     */
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<CommonResponse<Void>> handleApplicationException(ApplicationException e) {
        ErrorType errorType = e.getErrorType();
        log.error("ApplicationException: code={}, message={}", errorType.getCode(), errorType.getMessage(), e);
        return ResponseEntity.status(errorType.getHttpStatus())
                .body(CommonResponse.error(errorType));
    }

    /**
     * {@code @Valid} 검증 실패를 400(INVALID_INPUT_VALUE)으로 변환한다.
     * 필드 에러가 여러 개여도 팀 컨벤션에 따라 첫 번째 에러 메시지만 응답에 담는다.
     *
     * @param e Bean Validation 실패 정보
     * @return 첫 번째 필드 에러 메시지가 담긴 공통 응답
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(GlobalErrorType.INVALID_INPUT_VALUE.getMessage());
        log.error("MethodArgumentNotValidException: {}", message, e);
        return ResponseEntity.status(GlobalErrorType.INVALID_INPUT_VALUE.getHttpStatus())
                .body(CommonResponse.error(GlobalErrorType.INVALID_INPUT_VALUE, message));
    }

    /**
     * 컨트롤러가 지원하지 않는 HTTP 메서드로 들어온 요청을 405(METHOD_NOT_ALLOWED)로 변환한다.
     *
     * @param e 지원하지 않는 메서드로 호출된 요청 정보
     * @return METHOD_NOT_ALLOWED 공통 응답
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<CommonResponse<Void>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.error("HttpRequestMethodNotSupportedException", e);
        return ResponseEntity.status(GlobalErrorType.METHOD_NOT_ALLOWED.getHttpStatus())
                .body(CommonResponse.error(GlobalErrorType.METHOD_NOT_ALLOWED));
    }

    /**
     * 일치하는 컨트롤러나 정적 리소스가 없는 요청을 404(NOT_FOUND)로 변환한다.
     *
     * @param e 매칭되는 핸들러를 찾지 못한 요청 정보
     * @return NOT_FOUND 공통 응답
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<CommonResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.error("NoResourceFoundException", e);
        return ResponseEntity.status(GlobalErrorType.NOT_FOUND.getHttpStatus())
                .body(CommonResponse.error(GlobalErrorType.NOT_FOUND));
    }

    /**
     * 위 핸들러가 처리하지 못한 나머지 모든 예외를 500(INTERNAL_SERVER_ERROR)으로 변환한다.
     * 원인 파악을 위해 스택 트레이스를 ERROR 레벨로 로깅한다.
     *
     * @param e 처리되지 않은 예외
     * @return INTERNAL_SERVER_ERROR 공통 응답
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(GlobalErrorType.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(CommonResponse.error(GlobalErrorType.INTERNAL_SERVER_ERROR));
    }
}
