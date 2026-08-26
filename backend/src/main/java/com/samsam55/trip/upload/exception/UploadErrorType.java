package com.samsam55.trip.upload.exception;

import com.samsam55.trip.global.exception.ErrorType;
import org.springframework.http.HttpStatus;

public enum UploadErrorType implements ErrorType {

    INVALID_FILE_NAME(HttpStatus.BAD_REQUEST, "파일 이름이 올바르지 않습니다."),
    UNSUPPORTED_FILE_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 파일 형식입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    UploadErrorType(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getCode() {
        return name();
    }

    @Override
    public String getMessage() {
        return message;
    }
}
