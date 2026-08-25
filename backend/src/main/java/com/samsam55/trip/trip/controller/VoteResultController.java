package com.samsam55.trip.trip.controller;

import com.samsam55.trip.auth.annotation.CurrentParticipant;
import com.samsam55.trip.auth.dto.ParticipantPrincipal;
import com.samsam55.trip.global.common.CommonResponse;
import com.samsam55.trip.trip.dto.VoteResultResponseDto;
import com.samsam55.trip.trip.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/itinerary-items/{itemId}/vote-results")
@RequiredArgsConstructor
public class VoteResultController {

    private final ScheduleService scheduleService;

    /**
     * 현재 참여자가 조회할 수 있는 일정의 투표 결과를 반환한다.
     *
     * @param participant 현재 참여자
     * @param itemId 조회할 일정 항목의 식별자
     * @return 일정 정보와 참여자·선택지별 투표 현황을 담은 공통 응답
     * @throws com.samsam55.trip.global.exception.ApplicationException 일정이 없거나 권한이 없을 때(ITINERARY_ITEM_NOT_FOUND)
     */
    @GetMapping
    public CommonResponse<VoteResultResponseDto> findVoteResult(
            @CurrentParticipant ParticipantPrincipal participant,
            @PathVariable Long itemId
    ) {
        return CommonResponse.success(scheduleService.findVoteResult(participant, itemId));
    }
}
