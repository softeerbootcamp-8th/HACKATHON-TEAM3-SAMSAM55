package com.samsam55.trip.trip.controller;

import com.samsam55.trip.auth.annotation.Login;
import com.samsam55.trip.global.common.CommonResponse;
import com.samsam55.trip.trip.dto.TripListResponseDto;
import com.samsam55.trip.trip.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
