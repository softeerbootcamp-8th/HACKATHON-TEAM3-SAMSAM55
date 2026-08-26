package com.samsam55.trip.trip.controller;

import com.samsam55.trip.auth.annotation.Login;
import com.samsam55.trip.global.common.CommonResponse;
import com.samsam55.trip.trip.dto.VoteOptionSummaryDto;
import com.samsam55.trip.trip.dto.VoteOptionUpdateRequestDto;
import com.samsam55.trip.trip.service.VoteOptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vote-options")
@RequiredArgsConstructor
public class VoteOptionController {

    private final VoteOptionService voteOptionService;

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

    /**
     * 선택지의 이름·설명·이미지를 수정한다. 여행 방장만 호출할 수 있다.
     *
     * @param loginUserId 로그인한 회원의 식별자
     * @param voteOptionId 수정할 선택지의 식별자
     * @param request 이름·설명과 (미리 업로드한) 새 사진 S3 key가 담긴 수정 요청. imageKey가 없으면 기존 이미지를 유지한다.
     * @return 수정된 선택지가 담긴 200 응답
     */
    @PutMapping("/{voteOptionId}")
    public CommonResponse<VoteOptionSummaryDto> updateVoteOption(
            @Login Long loginUserId,
            @PathVariable Long voteOptionId,
            @Valid @RequestBody VoteOptionUpdateRequestDto request
    ) {
        return CommonResponse.success(voteOptionService.updateVoteOption(
                loginUserId, voteOptionId, request.name(), request.description(), request.imageKey()));
    }
}
