package com.samsam55.trip.trip.controller;

import com.samsam55.trip.auth.annotation.CurrentParticipant;
import com.samsam55.trip.auth.annotation.Login;
import com.samsam55.trip.auth.dto.ParticipantPrincipal;
import com.samsam55.trip.global.common.CommonResponse;
import com.samsam55.trip.trip.dto.MyVoteBatchRequestDto;
import com.samsam55.trip.trip.dto.MyVoteBatchResponseDto;
import com.samsam55.trip.trip.dto.VoteStartRequestDto;
import com.samsam55.trip.trip.dto.VoteStartResponseDto;
import com.samsam55.trip.trip.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/itinerary-items")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    /**
     * 준비 중인 일정 항목들을 부모 투표로 올린다. 여행 방장만 호출할 수 있고,
     * 요청에 담긴 일정 항목 중 하나라도 조건을 만족하지 못하면 전체가 롤백된다.
     *
     * @param loginUserId 로그인한 회원의 식별자
     * @param request 투표를 시작할 일정 항목 식별자 목록
     * @return 변경된 일정 항목들의 상태가 담긴 200 응답
     */
    @PostMapping("/vote/start")
    public ResponseEntity<CommonResponse<VoteStartResponseDto>> startVote(
            @Login Long loginUserId,
            @Valid @RequestBody VoteStartRequestDto request
    ) {
        return ResponseEntity.ok(CommonResponse.success(voteService.startVote(loginUserId, request.itemIds())));
    }

    /**
     * 참여자가 여러 일정 항목에 한 번에 투표하거나 기존 투표를 변경한다.
     * 요청에 담긴 일정 항목 중 하나라도 조건을 만족하지 못하면 전체가 롤백된다.
     *
     * @param principal 로그인한 참여자 정보
     * @param request 투표할 일정 항목과 선택할 옵션 목록
     * @return 저장된 투표 정보 목록과 다음으로 투표할 일정 항목 식별자가 담긴 200 응답
     */
    @PutMapping("/my-votes")
    public ResponseEntity<CommonResponse<MyVoteBatchResponseDto>> castVotes(
            @CurrentParticipant ParticipantPrincipal principal,
            @Valid @RequestBody MyVoteBatchRequestDto request
    ) {
        return ResponseEntity.ok(CommonResponse.success(voteService.castVotes(principal, request.votes())));
    }
}
