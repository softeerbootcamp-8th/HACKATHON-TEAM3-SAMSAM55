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

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<CommonResponse<Void>> handleApplicationException(ApplicationException e) {
        ErrorType errorType = e.getErrorType();
        log.error("ApplicationException: code={}, message={}", errorType.getCode(), errorType.getMessage(), e);
        return ResponseEntity.status(errorType.getHttpStatus())
                .body(CommonResponse.error(errorType));
    }

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

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<CommonResponse<Void>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.error("HttpRequestMethodNotSupportedException", e);
        return ResponseEntity.status(GlobalErrorType.METHOD_NOT_ALLOWED.getHttpStatus())
                .body(CommonResponse.error(GlobalErrorType.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<CommonResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.error("NoResourceFoundException", e);
        return ResponseEntity.status(GlobalErrorType.NOT_FOUND.getHttpStatus())
                .body(CommonResponse.error(GlobalErrorType.NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(GlobalErrorType.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(CommonResponse.error(GlobalErrorType.INTERNAL_SERVER_ERROR));
    }
}
