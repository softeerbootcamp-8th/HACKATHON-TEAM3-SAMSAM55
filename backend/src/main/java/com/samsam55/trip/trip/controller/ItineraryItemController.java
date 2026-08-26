package com.samsam55.trip.trip.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsam55.trip.auth.annotation.Login;
import com.samsam55.trip.global.common.CommonResponse;
import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.global.exception.GlobalErrorType;
import com.samsam55.trip.trip.dto.ItineraryItemCreateRequestDto;
import com.samsam55.trip.trip.dto.ItineraryItemCreateResponseDto;
import com.samsam55.trip.trip.dto.ItineraryItemDetailResponseDto;
import com.samsam55.trip.trip.dto.ItineraryItemUpdateRequestDto;
import com.samsam55.trip.trip.dto.VoteOptionCreateResponseDto;
import com.samsam55.trip.trip.dto.VoteStatusResponseDto;
import com.samsam55.trip.trip.service.ItineraryItemService;
import com.samsam55.trip.trip.service.VoteOptionService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class ItineraryItemController {

    // Jackson JSON 자동 설정을 쓰는 spring-boot-starter-web과 달리 이 프로젝트는
    // spring-boot-starter-webmvc만 써서 ObjectMapper가 스프링 빈으로 등록돼 있지 않다.
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ItineraryItemService itineraryItemService;
    private final VoteOptionService voteOptionService;
    private final Validator validator;

    /**
     * 일차에 새 일정 항목을 생성한다. 여행 방장만 호출할 수 있다.
     * {@code request}는 JSON 문자열 폼 필드로 받는다 — 브라우저가 만드는 multipart 요청에서
     * 문자열 파트는 Content-Type이 없어(application/octet-stream으로 취급됨) {@code @RequestPart}로
     * DTO에 바로 바인딩하면 415가 난다. 그래서 문자열로 받아 직접 파싱·검증한다.
     *
     * @param loginUserId 로그인한 회원의 식별자
     * @param dayId 일정 항목을 추가할 일차의 식별자
     * @param requestJson 일정 항목 생성 요청(JSON 문자열)
     * @param optionImages 선택지별 이미지(선택), {@code request.options}와 같은 순서로 매칭된다
     * @return 생성된 일정 항목이 담긴 201 응답
     * @throws ApplicationException {@code requestJson}이 올바른 JSON이 아니거나 필드 검증에 실패했을 때(INVALID_INPUT_VALUE)
     */
    @PostMapping(value = "/api/trip-days/{dayId}/itinerary-items", consumes = "multipart/form-data")
    public ResponseEntity<CommonResponse<ItineraryItemCreateResponseDto>> createItineraryItem(
            @Login Long loginUserId,
            @PathVariable Long dayId,
            @RequestParam("request") String requestJson,
            @RequestPart(value = "optionImages", required = false) List<MultipartFile> optionImages
    ) {
        ItineraryItemCreateRequestDto request = parseAndValidate(requestJson);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.success(
                        itineraryItemService.createItineraryItem(loginUserId, dayId, request, optionImages)));
    }

    private ItineraryItemCreateRequestDto parseAndValidate(String requestJson) {
        ItineraryItemCreateRequestDto request;
        try {
            request = objectMapper.readValue(requestJson, ItineraryItemCreateRequestDto.class);
        } catch (JsonProcessingException e) {
            throw new ApplicationException(GlobalErrorType.INVALID_INPUT_VALUE);
        }

        Set<ConstraintViolation<ItineraryItemCreateRequestDto>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ApplicationException(
                    GlobalErrorType.INVALID_INPUT_VALUE, violations.iterator().next().getMessage());
        }
        return request;
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

    // NOTE: 이 엔드포인트 자체는 멀티파트가 아니라 순수 JSON body라 아래 불안정성과 무관하다.
    // 다만 이 컨트롤러에 새 핸들러 메서드가 하나 늘어나는 것만으로도(내용과 무관하게),
    // createItineraryItem()의 request 파라미터가 springdoc 문서상 멀티파트 body 대신
    // 쿼리 파라미터로 잘못 분류되는 기존 버그 때문에, Orval이 생성하는
    // CreateItineraryItemBody 모양이 바뀔 수 있다 — 이 PR이 머지된 뒤 pnpm build가
    // frontend/src/routes/trips/$tripId/items/new.tsx에서 깨지면 이게 원인일 가능성이 높다.
    // 근본 원인(springdoc이 @RequestParam String을 멀티파트 엔드포인트에서 오분류하는 문제)은
    // 일정 만들기 담당자가 별도로 고치기로 했다.
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
