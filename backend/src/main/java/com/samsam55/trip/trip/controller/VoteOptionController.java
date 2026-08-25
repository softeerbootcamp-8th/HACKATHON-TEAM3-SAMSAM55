package com.samsam55.trip.trip.controller;

import com.samsam55.trip.auth.annotation.Login;
import com.samsam55.trip.trip.dto.VoteOptionImageDto;
import com.samsam55.trip.trip.service.VoteOptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vote-options")
@RequiredArgsConstructor
public class VoteOptionController {

    private final VoteOptionService voteOptionService;

    /**
     * 선택지에 등록된 이미지를 원본 바이트로 반환한다.
     *
     * @param loginUserId 로그인한 회원의 식별자
     * @param voteOptionId 조회할 선택지의 식별자
     * @return 이미지 바이트가 담긴 200 응답
     */
    @GetMapping("/{voteOptionId}/image")
    public ResponseEntity<byte[]> getImage(@Login Long loginUserId, @PathVariable Long voteOptionId) {
        VoteOptionImageDto image = voteOptionService.getImage(voteOptionId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.data());
    }
}
