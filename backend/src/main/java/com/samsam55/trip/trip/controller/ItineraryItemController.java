package com.samsam55.trip.trip.controller;

import com.samsam55.trip.auth.annotation.Login;
import com.samsam55.trip.global.common.CommonResponse;
import com.samsam55.trip.trip.dto.ItineraryItemCreateRequestDto;
import com.samsam55.trip.trip.dto.ItineraryItemCreateResponseDto;
import com.samsam55.trip.trip.dto.ItineraryItemDetailResponseDto;
import com.samsam55.trip.trip.dto.VoteOptionCreateResponseDto;
import com.samsam55.trip.trip.dto.VoteStatusResponseDto;
import com.samsam55.trip.trip.service.ItineraryItemService;
import com.samsam55.trip.trip.service.VoteOptionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class ItineraryItemController {

    private final ItineraryItemService itineraryItemService;
    private final VoteOptionService voteOptionService;

    /**
     * 일차에 새 일정 항목을 생성한다. 여행 방장만 호출할 수 있다.
     *
     * @param loginUserId 로그인한 회원의 식별자
     * @param dayId 일정 항목을 추가할 일차의 식별자
     * @param request 일정 항목 생성 요청
     * @param optionImages 선택지별 이미지(선택), {@code request.options}와 같은 순서로 매칭된다
     * @return 생성된 일정 항목이 담긴 201 응답
     */
    @PostMapping(value = "/api/trip-days/{dayId}/itinerary-items", consumes = "multipart/form-data")
    public ResponseEntity<CommonResponse<ItineraryItemCreateResponseDto>> createItineraryItem(
            @Login Long loginUserId,
            @PathVariable Long dayId,
            @Valid @RequestPart("request") ItineraryItemCreateRequestDto request,
            @RequestPart(value = "optionImages", required = false) List<MultipartFile> optionImages
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.success(
                        itineraryItemService.createItineraryItem(loginUserId, dayId, request, optionImages)));
    }

    /**
     * 일정 항목 상세를 조회한다. 여행 방장만 호출할 수 있다.
     *
     * @param loginUserId 로그인한 회원의 식별자
     * @param itemId 조회할 일정 항목의 식별자
     * @return 일정 항목 상세가 담긴 200 응답
     */
    @GetMapping("/api/itinerary-items/{itemId}")
    public CommonResponse<ItineraryItemDetailResponseDto> getItineraryItem(
            @Login Long loginUserId,
            @PathVariable Long itemId
    ) {
        return CommonResponse.success(itineraryItemService.getItineraryItem(loginUserId, itemId));
    }

    /**
     * 일정 항목의 투표 진행 현황을 조회한다. 여행 방장만 호출할 수 있다.
     *
     * @param loginUserId 로그인한 회원의 식별자
     * @param itemId 조회할 일정 항목의 식별자
     * @return 참여자별 투표 여부와 선택지별 득표 현황이 담긴 200 응답
     */
    @GetMapping("/api/itinerary-items/{itemId}/vote-status")
    public CommonResponse<VoteStatusResponseDto> getVoteStatus(
            @Login Long loginUserId,
            @PathVariable Long itemId
    ) {
        return CommonResponse.success(itineraryItemService.getVoteStatus(loginUserId, itemId));
    }

    /**
     * 일정 항목에 투표 선택지를 추가한다. 여행 방장만 호출할 수 있다.
     * decisionType이 HOST_PICK이면 추가된 선택지가 즉시 확정된다.
     *
     * @param loginUserId 로그인한 회원의 식별자
     * @param itemId 선택지를 추가할 일정 항목의 식별자
     * @param name 선택지 이름
     * @param image 선택지 이미지(선택)
     * @return 생성된 선택지가 담긴 201 응답
     */
    @PostMapping(value = "/api/itinerary-items/{itemId}/vote-options", consumes = "multipart/form-data")
    public ResponseEntity<CommonResponse<VoteOptionCreateResponseDto>> createVoteOption(
            @Login Long loginUserId,
            @PathVariable Long itemId,
            @RequestParam String name,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.success(
                        voteOptionService.createVoteOption(loginUserId, itemId, name, image)));
    }
}
