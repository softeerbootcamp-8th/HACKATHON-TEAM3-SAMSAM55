package com.samsam55.trip.trip.controller;

import com.samsam55.trip.global.common.CommonResponse;
import com.samsam55.trip.trip.dto.InviteJoinRequestDto;
import com.samsam55.trip.trip.dto.InviteJoinResponseDto;
import com.samsam55.trip.trip.dto.InviteVerifyResponseDto;
import com.samsam55.trip.trip.service.InviteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    /**
     * 참여자 슬롯 하나를 선점하고 세션과 복구용 쿠키를 발급한다.
     *
     * @param inviteCode 초대 코드
     * @param request 선점할 참여자 슬롯 ID
     * @param servletRequest 세션을 생성할 현재 HTTP 요청
     * @param servletResponse 복구용 쿠키를 내려줄 현재 HTTP 응답
     * @return 선점한 참여자 정보가 담긴 200 응답
     */
    @PostMapping("/{inviteCode}/join")
    public ResponseEntity<CommonResponse<InviteJoinResponseDto>> join(
            @PathVariable String inviteCode,
            @Valid @RequestBody InviteJoinRequestDto request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        InviteJoinResponseDto response = inviteService.join(inviteCode, request, servletRequest, servletResponse);
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}
