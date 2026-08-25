package com.samsam55.trip.trip.service;

import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.trip.dto.ItineraryItemStatusDto;
import com.samsam55.trip.trip.dto.VoteStartResponseDto;
import com.samsam55.trip.trip.entity.ItineraryItem;
import com.samsam55.trip.trip.entity.ItineraryItemDecisionType;
import com.samsam55.trip.trip.entity.ItineraryItemStatus;
import com.samsam55.trip.trip.exception.TripErrorType;
import com.samsam55.trip.trip.repository.ItineraryItemRepository;
import com.samsam55.trip.trip.repository.VoteOptionRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VoteService {

    private static final int MIN_VOTE_OPTION_COUNT = 2;

    private final ItineraryItemRepository itineraryItemRepository;
    private final VoteOptionRepository voteOptionRepository;

    /**
     * 준비 중인 일정 항목들을 한 번에 부모 투표로 올린다.
     * 목록에 담긴 일정 항목 중 하나라도 조건을 만족하지 못하면 전체가 롤백된다.
     *
     * @param loginUserId 요청한 회원의 식별자
     * @param itemIds 투표를 시작할 일정 항목 식별자 목록
     * @return 변경된 일정 항목들의 상태 목록
     * @throws ApplicationException 일정 항목을 찾을 수 없을 때(ITINERARY_ITEM_NOT_FOUND)
     * @throws ApplicationException 요청자가 여행 방장이 아닐 때(NOT_TRIP_HOST)
     * @throws ApplicationException 결정 방식이 투표가 아닐 때(ITINERARY_ITEM_NOT_VOTE_TYPE)
     * @throws ApplicationException 이미 투표가 시작됐거나 확정된 일정일 때(ITINERARY_ITEM_ALREADY_OPENED)
     * @throws ApplicationException 선택지가 2개 미만일 때(VOTE_OPTION_COUNT_INSUFFICIENT)
     */
    @Transactional
    public VoteStartResponseDto startVote(Long loginUserId, List<Long> itemIds) {
        List<ItineraryItemStatusDto> results = new ArrayList<>();
        for (Long itemId : itemIds) {
            ItineraryItem itineraryItem = itineraryItemRepository.findById(itemId)
                    .orElseThrow(() -> withItemId(TripErrorType.ITINERARY_ITEM_NOT_FOUND, itemId));

            if (!itineraryItem.getTripDay().getTrip().getHostUser().getId().equals(loginUserId)) {
                throw withItemId(TripErrorType.NOT_TRIP_HOST, itemId);
            }
            if (itineraryItem.getDecisionType() != ItineraryItemDecisionType.VOTE) {
                throw withItemId(TripErrorType.ITINERARY_ITEM_NOT_VOTE_TYPE, itemId);
            }
            if (itineraryItem.getStatus() != ItineraryItemStatus.PENDING) {
                throw withItemId(TripErrorType.ITINERARY_ITEM_ALREADY_OPENED, itemId);
            }
            if (voteOptionRepository.countByItineraryItemId(itemId) < MIN_VOTE_OPTION_COUNT) {
                throw withItemId(TripErrorType.VOTE_OPTION_COUNT_INSUFFICIENT, itemId);
            }

            itineraryItem.openVote();
            results.add(ItineraryItemStatusDto.from(itineraryItem));
        }
        return VoteStartResponseDto.from(results);
    }

    private ApplicationException withItemId(TripErrorType errorType, Long itemId) {
        return new ApplicationException(errorType, errorType.getMessage() + " (itemId: " + itemId + ")");
    }
}
