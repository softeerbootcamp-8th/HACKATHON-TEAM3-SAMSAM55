package com.samsam55.trip.trip.controller;

import com.samsam55.trip.global.common.CommonResponse;
import com.samsam55.trip.trip.dto.InviteVerifyResponseDto;
import com.samsam55.trip.trip.service.InviteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invites")
@RequiredArgsConstructor
public class InviteController {

    private final InviteService inviteService;

    /**
     * 초대 코드가 가리키는 여행과 참여자 슬롯 선점 현황을 조회한다.
     *
     * @param inviteCode 초대 코드
     * @return 여행 정보와 참여자 슬롯 목록이 담긴 200 응답
     */
    @GetMapping("/{inviteCode}")
    public ResponseEntity<CommonResponse<InviteVerifyResponseDto>> verify(@PathVariable String inviteCode) {
        return ResponseEntity.ok(CommonResponse.success(inviteService.verify(inviteCode)));
    }
}
