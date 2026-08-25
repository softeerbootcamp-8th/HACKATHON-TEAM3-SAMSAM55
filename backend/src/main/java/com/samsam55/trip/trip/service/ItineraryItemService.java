package com.samsam55.trip.trip.service;

import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.trip.ai.VoteOptionDescriptionGenerator;
import com.samsam55.trip.trip.dto.ItineraryItemCreateRequestDto;
import com.samsam55.trip.trip.dto.ItineraryItemCreateResponseDto;
import com.samsam55.trip.trip.dto.ItineraryItemDetailResponseDto;
import com.samsam55.trip.trip.dto.VoteOptionSummaryDto;
import com.samsam55.trip.trip.dto.VoteStartItemResultDto;
import com.samsam55.trip.trip.dto.VoteStartResponseDto;
import com.samsam55.trip.trip.entity.ItineraryItem;
import com.samsam55.trip.trip.entity.ItineraryItemDecisionType;
import com.samsam55.trip.trip.entity.ItineraryItemStatus;
import com.samsam55.trip.trip.entity.TripDay;
import com.samsam55.trip.trip.entity.VoteOption;
import com.samsam55.trip.trip.exception.TripErrorType;
import com.samsam55.trip.trip.repository.ItineraryItemRepository;
import com.samsam55.trip.trip.repository.TripDayRepository;
import com.samsam55.trip.trip.repository.VoteOptionRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ItineraryItemService {

    private static final int MIN_VOTE_OPTION_COUNT = 2;
    private static final int MAX_VOTE_OPTION_COUNT = 4;

    private final TripDayRepository tripDayRepository;
    private final ItineraryItemRepository itineraryItemRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final VoteOptionDescriptionGenerator descriptionGenerator;

    /**
     * 일차에 새 일정 항목을 생성한다. 결정 방식이 VOTE이고 선택지가 있으면
     * 선택지별 AI 설명까지 동기로 생성한 뒤 응답한다.
     *
     * @param loginUserId 요청한 회원의 식별자
     * @param tripDayId 일정 항목을 추가할 일차의 식별자
     * @param request 이름·카테고리·결정 방식·선택지가 담긴 생성 요청
     * @param optionImages 선택지별 이미지. {@code options}와 같은 순서로 매칭되며, 특정 선택지에
     *                      이미지가 없으면 그 자리에 빈 파일이 오거나 리스트가 더 짧을 수 있다.
     * @return 생성된 일정 항목과 선택지 목록
     * @throws ApplicationException 일차를 찾을 수 없을 때(TRIP_DAY_NOT_FOUND)
     * @throws ApplicationException 요청자가 여행 방장이 아닐 때(NOT_TRIP_HOST)
     * @throws ApplicationException VOTE 선택지가 4개를 초과할 때(VOTE_OPTION_COUNT_EXCEEDED)
     */
    @Transactional
    public ItineraryItemCreateResponseDto createItineraryItem(
            Long loginUserId, Long tripDayId, ItineraryItemCreateRequestDto request, List<MultipartFile> optionImages) {
        // 같은 일차에 동시에 생성 요청이 오면 sortOrder가 겹칠 수 있어 TripDay row를 잠그고 진행
        TripDay tripDay = tripDayRepository.findByIdForUpdate(tripDayId)
                .orElseThrow(() -> new ApplicationException(TripErrorType.TRIP_DAY_NOT_FOUND));

        if (!tripDay.getTrip().getHostUser().getId().equals(loginUserId)) {
            throw new ApplicationException(TripErrorType.NOT_TRIP_HOST);
        }

        // HOST_PICK은 선택지를 무시하고, VOTE는 최대 4개까지만 허용
        ItineraryItemDecisionType decisionType = ItineraryItemDecisionType.valueOf(request.decisionType());
        List<String> optionNames = decisionType == ItineraryItemDecisionType.VOTE
                ? Optional.ofNullable(request.options()).orElseGet(List::of)
                : List.of();
        if (optionNames.size() > MAX_VOTE_OPTION_COUNT) {
            throw new ApplicationException(TripErrorType.VOTE_OPTION_COUNT_EXCEEDED);
        }

        // 일정 항목은 해당 일차의 마지막 순서 다음으로 생성
        int sortOrder = itineraryItemRepository.findMaxSortOrderByTripDayId(tripDayId) + 1;
        ItineraryItem itineraryItem = itineraryItemRepository.save(new ItineraryItem(
                tripDay, request.name(), request.category(), decisionType,
                ItineraryItemStatus.PENDING, sortOrder, null
        ));

        // 선택지별로 AI 설명을 동기 생성하고, 있으면 이미지도 함께 저장
        List<MultipartFile> images = optionImages != null ? optionImages : List.of();
        List<VoteOptionSummaryDto> voteOptions = IntStream.range(0, optionNames.size())
                .mapToObj(i -> {
                    String optionName = optionNames.get(i);
                    MultipartFile image = i < images.size() ? images.get(i) : null;
                    return voteOptionRepository.save(new VoteOption(
                            itineraryItem,
                            optionName,
                            descriptionGenerator.generate(optionName),
                            VoteOptionDescriptionGenerator.SOURCE,
                            hasContent(image) ? readBytes(image) : null,
                            hasContent(image) ? image.getContentType() : null
                    ));
                })
                .map(VoteOptionSummaryDto::from)
                .toList();

        return ItineraryItemCreateResponseDto.from(itineraryItem, voteOptions);
    }

    /**
     * 일정 항목 상세를 조회한다.
     *
     * @param loginUserId 요청한 회원의 식별자
     * @param itemId 조회할 일정 항목의 식별자
     * @return 일정 항목과 선택지 목록
     * @throws ApplicationException 일정 항목을 찾을 수 없을 때(ITINERARY_ITEM_NOT_FOUND)
     * @throws ApplicationException 요청자가 여행 방장이 아닐 때(NOT_TRIP_HOST)
     */
    @Transactional(readOnly = true)
    public ItineraryItemDetailResponseDto getItineraryItem(Long loginUserId, Long itemId) {
        ItineraryItem itineraryItem = itineraryItemRepository.findById(itemId)
                .orElseThrow(() -> new ApplicationException(TripErrorType.ITINERARY_ITEM_NOT_FOUND));

        if (!itineraryItem.getTripDay().getTrip().getHostUser().getId().equals(loginUserId)) {
            throw new ApplicationException(TripErrorType.NOT_TRIP_HOST);
        }

        List<VoteOptionSummaryDto> voteOptions = voteOptionRepository.findByItineraryItem(itineraryItem).stream()
                .map(VoteOptionSummaryDto::from)
                .toList();

        return ItineraryItemDetailResponseDto.from(itineraryItem, voteOptions);
    }

    /**
     * 일정 항목들을 한 번에 투표 상태로 전환한다. 하나라도 조건을 못 채우면 전체가 실패한다.
     *
     * @param loginUserId 요청한 회원의 식별자
     * @param itemIds 투표를 시작할 일정 항목 식별자 목록
     * @return 전환된 일정 항목들의 상태
     * @throws ApplicationException 일정 항목을 찾을 수 없을 때(ITINERARY_ITEM_NOT_FOUND)
     * @throws ApplicationException 요청자가 여행 방장이 아닐 때(NOT_TRIP_HOST)
     * @throws ApplicationException 이미 투표가 시작됐거나 VOTE 방식이 아닐 때(VOTE_ALREADY_STARTED)
     * @throws ApplicationException 선택지가 2개 미만일 때(VOTE_OPTION_COUNT_INSUFFICIENT)
     */
    @Transactional
    public VoteStartResponseDto startVote(Long loginUserId, List<Long> itemIds) {
        List<ItineraryItem> itineraryItems = itemIds.stream()
                .map(itemId -> itineraryItemRepository.findById(itemId)
                        .orElseThrow(() -> new ApplicationException(TripErrorType.ITINERARY_ITEM_NOT_FOUND)))
                .toList();

        for (ItineraryItem itineraryItem : itineraryItems) {
            if (!itineraryItem.getTripDay().getTrip().getHostUser().getId().equals(loginUserId)) {
                throw new ApplicationException(TripErrorType.NOT_TRIP_HOST);
            }
            if (itineraryItem.getDecisionType() != ItineraryItemDecisionType.VOTE
                    || itineraryItem.getStatus() != ItineraryItemStatus.PENDING) {
                throw new ApplicationException(TripErrorType.VOTE_ALREADY_STARTED);
            }
            if (voteOptionRepository.countByItineraryItem(itineraryItem) < MIN_VOTE_OPTION_COUNT) {
                throw new ApplicationException(TripErrorType.VOTE_OPTION_COUNT_INSUFFICIENT);
            }
        }

        itineraryItems.forEach(ItineraryItem::startVoting);

        return new VoteStartResponseDto(itineraryItems.stream().map(VoteStartItemResultDto::from).toList());
    }

    private boolean hasContent(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
