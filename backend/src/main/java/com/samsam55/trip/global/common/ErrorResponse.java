package com.samsam55.trip.global.common;

import com.samsam55.trip.global.exception.ErrorType;

public record ErrorResponse(String code, String message) {

    public static ErrorResponse of(ErrorType errorType) {
        return new ErrorResponse(errorType.getCode(), errorType.getMessage());
    }

    public static ErrorResponse of(ErrorType errorType, String message) {
        return new ErrorResponse(errorType.getCode(), message);
    }
}
