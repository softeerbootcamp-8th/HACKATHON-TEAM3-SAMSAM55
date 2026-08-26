package com.samsam55.trip.trip.exception;

import com.samsam55.trip.global.exception.ErrorType;
import org.springframework.http.HttpStatus;

public enum TripErrorType implements ErrorType {

    TRIP_NOT_FOUND(HttpStatus.NOT_FOUND, "여행을 찾을 수 없습니다."),
    INVALID_TRIP_PERIOD(HttpStatus.BAD_REQUEST, "여행 기간이 올바르지 않습니다."),
    INVITE_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "유효하지 않은 초대 코드입니다."),
    PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "참여자를 찾을 수 없습니다."),
    PARTICIPANT_ALREADY_JOINED(HttpStatus.CONFLICT, "이미 다른 사람이 참여한 역할입니다."),
    ALREADY_PARTICIPANT(HttpStatus.CONFLICT, "이미 다른 역할로 참여했습니다."),
    PARTICIPANT_LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "참여자 인증이 필요합니다."),
    TRIP_DAY_NOT_FOUND(HttpStatus.NOT_FOUND, "일차를 찾을 수 없습니다."),
    ITINERARY_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "일정 항목을 찾을 수 없습니다."),
    NOT_TRIP_HOST(HttpStatus.FORBIDDEN, "여행의 방장만 사용할 수 있습니다."),
    VOTE_OPTION_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "선택지는 최대 4개까지 등록할 수 있습니다."),
    VOTE_OPTION_COUNT_INSUFFICIENT(HttpStatus.CONFLICT, "선택지가 2개 이상이어야 투표를 시작할 수 있습니다."),
    VOTE_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "선택지를 찾을 수 없습니다."),
    ITINERARY_ITEM_NOT_VOTE_TYPE(HttpStatus.CONFLICT, "부모님과 투표로 정하는 일정만 투표를 시작할 수 있습니다."),
    ITINERARY_ITEM_ALREADY_OPENED(HttpStatus.CONFLICT, "이미 투표가 시작된 일정입니다."),
    ITINERARY_ITEM_NOT_VOTABLE(HttpStatus.CONFLICT, "아직 투표를 시작하지 않았거나 이미 확정된 일정입니다."),
    ITINERARY_ITEM_NOT_CONFIRMED(HttpStatus.CONFLICT, "확정된 일정이 아닙니다."),
    TRIP_PARTICIPANT_MISMATCH(HttpStatus.FORBIDDEN, "다른 여행의 참여자입니다."),
    VOTE_ALREADY_STARTED(HttpStatus.CONFLICT, "투표가 시작된 항목은 수정할 수 없습니다."),
    VOTE_OPTION_SELECTION_REQUIRED(HttpStatus.CONFLICT, "선택지가 여러 개일 때는 하나를 선택해야 합니다.");

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
