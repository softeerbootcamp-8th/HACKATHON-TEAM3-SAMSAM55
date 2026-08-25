package com.samsam55.trip.trip.exception;

import com.samsam55.trip.global.exception.ErrorType;
import org.springframework.http.HttpStatus;

public enum TripErrorType implements ErrorType {

    TRIP_DAY_NOT_FOUND(HttpStatus.NOT_FOUND, "일차를 찾을 수 없습니다."),
    NOT_TRIP_HOST(HttpStatus.FORBIDDEN, "여행의 방장만 사용할 수 있습니다."),
    VOTE_OPTION_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "선택지는 최대 4개까지 등록할 수 있습니다."),
    VOTE_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "선택지를 찾을 수 없습니다."),
    VOTE_OPTION_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "선택지에 등록된 이미지가 없습니다.");

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
