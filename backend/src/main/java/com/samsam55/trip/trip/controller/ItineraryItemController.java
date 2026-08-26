package com.samsam55.trip.trip.controller;

import com.samsam55.trip.auth.annotation.Login;
import com.samsam55.trip.global.common.CommonResponse;
import com.samsam55.trip.trip.dto.ItineraryItemCreateRequestDto;
import com.samsam55.trip.trip.dto.ItineraryItemCreateResponseDto;
import com.samsam55.trip.trip.dto.ItineraryItemDetailResponseDto;
import com.samsam55.trip.trip.dto.ItineraryItemReorderRequestDto;
import com.samsam55.trip.trip.dto.ItineraryItemUpdateRequestDto;
import com.samsam55.trip.trip.dto.VoteOptionCreateRequestDto;
import com.samsam55.trip.trip.dto.VoteOptionCreateResponseDto;
import com.samsam55.trip.trip.dto.VoteStatusResponseDto;
import com.samsam55.trip.trip.service.ItineraryItemService;
import com.samsam55.trip.trip.service.VoteOptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
     * @param request 이름·카테고리·결정 방식·선택지(사진은 미리 업로드한 S3 key로 전달)가 담긴 생성 요청
     * @return 생성된 일정 항목이 담긴 201 응답
     */
    @PostMapping("/api/trip-days/{dayId}/itinerary-items")
    public ResponseEntity<CommonResponse<ItineraryItemCreateResponseDto>> createItineraryItem(
            @Login Long loginUserId,
            @PathVariable Long dayId,
            @Valid @RequestBody ItineraryItemCreateRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.success(
                        itineraryItemService.createItineraryItem(loginUserId, dayId, request)));
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
     * 일정 항목을 삭제한다. 여행 방장만 호출할 수 있다. 상태와 무관하게 삭제할 수 있다.
     *
     * @param loginUserId 로그인한 회원의 식별자
     * @param itemId 삭제할 일정 항목의 식별자
     * @return 데이터가 없는 200 응답
     */
    @DeleteMapping("/api/itinerary-items/{itemId}")
    public CommonResponse<Void> deleteItineraryItem(
            @Login Long loginUserId,
            @PathVariable Long itemId
    ) {
        itineraryItemService.deleteItineraryItem(loginUserId, itemId);
        return CommonResponse.empty();
    }

    /**
     * 일정 항목의 이름·카테고리·결정 방식을 수정한다. 여행 방장만 호출할 수 있고,
     * 투표가 시작되기 전(PENDING)에만 수정할 수 있다.
     *
     * @param loginUserId 로그인한 회원의 식별자
     * @param itemId 수정할 일정 항목의 식별자
     * @param updateRequest 이름·카테고리·결정 방식이 담긴 수정 요청
     * @return 수정된 일정 항목 상세가 담긴 200 응답
     */
    @PutMapping("/api/itinerary-items/{itemId}")
    public CommonResponse<ItineraryItemDetailResponseDto> updateItineraryItem(
            @Login Long loginUserId,
            @PathVariable Long itemId,
            @Valid @RequestBody ItineraryItemUpdateRequestDto updateRequest
    ) {
        return CommonResponse.success(itineraryItemService.updateItineraryItem(loginUserId, itemId, updateRequest));
    }

    /**
     * 같은 일차 안에서 일정 항목의 순서를 바꾼다. 여행 방장만 호출할 수 있다.
     *
     * @param loginUserId 로그인한 회원의 식별자
     * @param dayId 순서를 바꿀 일차의 식별자
     * @param request 새 순서대로 나열한 일정 항목 식별자 목록
     * @return 데이터가 없는 200 응답
     */
    @PutMapping("/api/trip-days/{dayId}/itinerary-items/order")
    public CommonResponse<Void> reorderItineraryItems(
            @Login Long loginUserId,
            @PathVariable Long dayId,
            @Valid @RequestBody ItineraryItemReorderRequestDto request
    ) {
        itineraryItemService.reorderItineraryItems(loginUserId, dayId, request.itemIds());
        return CommonResponse.empty();
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
     * decisionType과 무관하게 추가된 선택지는 PENDING 상태로 남고, 방장이 별도로
     * 확정 API를 호출해야 일정이 확정된다.
     *
     * @param loginUserId 로그인한 회원의 식별자
     * @param itemId 선택지를 추가할 일정 항목의 식별자
     * @param request 선택지 이름과 (미리 업로드한) 사진 S3 key가 담긴 생성 요청
     * @return 생성된 선택지가 담긴 201 응답
     */
    @PostMapping("/api/itinerary-items/{itemId}/vote-options")
    public ResponseEntity<CommonResponse<VoteOptionCreateResponseDto>> createVoteOption(
            @Login Long loginUserId,
            @PathVariable Long itemId,
            @Valid @RequestBody VoteOptionCreateRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.success(
                        voteOptionService.createVoteOption(loginUserId, itemId, request.name(), request.imageKey())));
    }
}
