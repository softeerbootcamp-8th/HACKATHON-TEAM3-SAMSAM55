package com.samsam55.trip.trip.exception;

import com.samsam55.trip.global.exception.ErrorType;
import org.springframework.http.HttpStatus;

public enum TripErrorType implements ErrorType {

    TRIP_NOT_FOUND(HttpStatus.NOT_FOUND, "여행을 찾을 수 없습니다."),
    INVALID_TRIP_PERIOD(HttpStatus.BAD_REQUEST, "여행 기간이 올바르지 않습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    TripErrorType(HttpStatus httpStatus, String message) {
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
