package com.samsam55.trip.trip.controller;

import com.samsam55.trip.auth.annotation.Login;
import com.samsam55.trip.global.common.CommonResponse;
import com.samsam55.trip.trip.dto.TripCreateRequestDto;
import com.samsam55.trip.trip.dto.TripCreateResponseDto;
import com.samsam55.trip.trip.dto.TripListResponseDto;
import com.samsam55.trip.trip.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    /**
     * 현재 로그인한 사용자가 방장인 여행 목록을 조회한다.
     *
     * @param userId 세션에서 해석한 로그인 사용자의 ID
     * @return 여행 목록을 담은 공통 응답
     */
    @GetMapping
    public CommonResponse<TripListResponseDto> findTrips(@Login Long userId) {
        return CommonResponse.success(tripService.findTrips(userId));
    }

    /**
     * 현재 로그인한 사용자를 방장으로 여행을 생성한다.
     *
     * @param userId 세션에서 해석한 로그인 사용자의 ID
     * @param request 여행 생성 정보
     * @return 생성된 여행 정보를 담은 201 응답
     */
    @PostMapping
    public ResponseEntity<CommonResponse<TripCreateResponseDto>> createTrip(
            @Login Long userId,
            @Valid @RequestBody TripCreateRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.success(tripService.createTrip(userId, request)));
    }

    /**
     * 현재 로그인한 사용자가 방장인 여행을 삭제한다.
     *
     * @param userId 세션에서 해석한 로그인 사용자의 ID
     * @param tripId 삭제할 여행의 ID
     * @return 데이터가 없는 공통 성공 응답
     * @throws com.samsam55.trip.global.exception.ApplicationException 여행이 없거나 방장이 아닐 때(TRIP_NOT_FOUND)
     */
    @DeleteMapping("/{tripId}")
    public CommonResponse<Void> deleteTrip(@Login Long userId, @PathVariable Long tripId) {
        tripService.deleteTrip(userId, tripId);
        return CommonResponse.empty();
    }
}
