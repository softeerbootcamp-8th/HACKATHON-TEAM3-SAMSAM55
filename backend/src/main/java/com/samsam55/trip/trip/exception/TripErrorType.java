package com.samsam55.trip.trip.exception;

import com.samsam55.trip.global.exception.ErrorType;
import org.springframework.http.HttpStatus;

public enum TripErrorType implements ErrorType {

    INVITE_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "유효하지 않은 초대 코드입니다."),
    PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "참여자를 찾을 수 없습니다."),
    PARTICIPANT_ALREADY_JOINED(HttpStatus.CONFLICT, "이미 다른 사람이 참여한 역할입니다.");

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
