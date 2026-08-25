package com.samsam55.trip.global.exception;

import org.springframework.http.HttpStatus;

public interface ErrorType {

    HttpStatus getHttpStatus();

    String getCode();

    String getMessage();
}
