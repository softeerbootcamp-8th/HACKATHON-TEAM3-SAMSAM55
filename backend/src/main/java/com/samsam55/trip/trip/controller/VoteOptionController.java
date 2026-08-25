package com.samsam55.trip.trip.controller;

import com.samsam55.trip.auth.annotation.Login;
import com.samsam55.trip.auth.dto.AuthMeResponseDto;
import com.samsam55.trip.auth.service.AuthService;
import com.samsam55.trip.global.common.CommonResponse;
import com.samsam55.trip.trip.dto.VoteOptionImageDto;
import com.samsam55.trip.trip.service.VoteOptionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vote-options")
@RequiredArgsConstructor
public class VoteOptionController {

    private final VoteOptionService voteOptionService;
    private final AuthService authService;

    /**
     * 선택지에 등록된 이미지를 원본 바이트로 반환한다.
     *
     * @param request 현재 HTTP 요청
     * @param voteOptionId 조회할 선택지의 식별자
     * @return 이미지 바이트가 담긴 200 응답
     * @throws com.samsam55.trip.global.exception.ApplicationException 선택지가 없거나 권한이 없을 때(VOTE_OPTION_NOT_FOUND)
     * @throws com.samsam55.trip.global.exception.ApplicationException 선택지에 이미지가 없을 때(VOTE_OPTION_IMAGE_NOT_FOUND)
     */
    @GetMapping("/{voteOptionId}/image")
    public ResponseEntity<byte[]> getImage(
            HttpServletRequest request,
            @PathVariable Long voteOptionId
    ) {
        AuthMeResponseDto actor = authService.me(request);
        VoteOptionImageDto image = voteOptionService.getImage(actor, voteOptionId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.data());
    }

    /**
     * 선택지를 삭제한다. 여행 방장만 호출할 수 있다.
     *
     * @param loginUserId 로그인한 회원의 식별자
     * @param voteOptionId 삭제할 선택지의 식별자
     * @return 데이터가 없는 200 응답
     */
    @DeleteMapping("/{voteOptionId}")
    public CommonResponse<Void> deleteVoteOption(@Login Long loginUserId, @PathVariable Long voteOptionId) {
        voteOptionService.deleteVoteOption(loginUserId, voteOptionId);
        return CommonResponse.empty();
    }
}
