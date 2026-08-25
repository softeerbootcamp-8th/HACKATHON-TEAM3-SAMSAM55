package com.samsam55.trip.global.common;

import com.samsam55.trip.global.exception.ErrorType;

public record CommonResponse<T>(boolean success, T data, ErrorResponse error) {

    public static <T> CommonResponse<T> success(T data) {
        return new CommonResponse<>(true, data, null);
    }

    public static CommonResponse<Void> empty() {
        return new CommonResponse<>(true, null, null);
    }

    public static <T> CommonResponse<T> error(ErrorType errorType) {
        return new CommonResponse<>(false, null, ErrorResponse.of(errorType));
    }

    public static <T> CommonResponse<T> error(ErrorType errorType, String message) {
        return new CommonResponse<>(false, null, ErrorResponse.of(errorType, message));
    }
}
