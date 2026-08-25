package com.samsam55.trip.trip.controller;

import com.samsam55.trip.auth.annotation.CurrentActor;
import com.samsam55.trip.auth.dto.ActorPrincipal;
import com.samsam55.trip.global.common.CommonResponse;
import com.samsam55.trip.trip.dto.ScheduleResponseDto;
import com.samsam55.trip.trip.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips/{tripId}/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * 현재 인증 주체가 조회할 수 있는 여행 일정을 반환한다.
     *
     * @param actor 현재 인증 주체
     * @param tripId 조회할 여행의 식별자
     * @return 날짜별 일정과 투표 진행 현황을 담은 공통 응답
     * @throws com.samsam55.trip.global.exception.ApplicationException 여행이 없거나 권한이 없을 때(TRIP_NOT_FOUND)
     */
    @GetMapping
    public CommonResponse<ScheduleResponseDto> findSchedule(
            @CurrentActor ActorPrincipal actor,
            @PathVariable Long tripId
    ) {
        return CommonResponse.success(scheduleService.findSchedule(actor, tripId));
    }
}
