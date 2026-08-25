package com.samsam55.trip.trip.controller;

import com.samsam55.trip.auth.annotation.Login;
import com.samsam55.trip.global.common.CommonResponse;
import com.samsam55.trip.trip.dto.ItineraryItemCreateRequestDto;
import com.samsam55.trip.trip.dto.ItineraryItemCreateResponseDto;
import com.samsam55.trip.trip.service.ItineraryItemService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/trip-days/{dayId}/itinerary-items")
@RequiredArgsConstructor
public class ItineraryItemController {

    private final ItineraryItemService itineraryItemService;

    /**
     * 일차에 새 일정 항목을 생성한다. 여행 방장만 호출할 수 있다.
     *
     * @param loginUserId 로그인한 회원의 식별자
     * @param dayId 일정 항목을 추가할 일차의 식별자
     * @param request 일정 항목 생성 요청
     * @param optionImages 선택지별 이미지(선택), {@code request.options}와 같은 순서로 매칭된다
     * @return 생성된 일정 항목이 담긴 201 응답
     */
    // TODO(일정 항목 생성 담당자): 멀티파트 "request" 파트가 OpenAPI 스펙에 int64로
    // 잘못 노출되고 있다. Orval이 생성한 프론트 클라이언트도 그대로 깨져서
    // createItineraryItemBody?: number 타입에 .toString()을 호출해 pnpm build(tsc -b)가
    // 실패한다 (src/api/generated/itinerary-item-controller/itinerary-item-controller.ts).
    // @Schema 등으로 request 파트 타입을 명시해서 springdoc이 ItineraryItemCreateRequestDto로
    // 올바르게 인식하게 고친 뒤 pnpm api:generate 재실행 필요.
    @PostMapping(consumes = "multipart/form-data")
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
}
