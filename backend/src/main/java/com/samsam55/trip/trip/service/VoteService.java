package com.samsam55.trip.trip.service;

import com.samsam55.trip.auth.dto.ParticipantPrincipal;
import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.trip.dto.ItineraryItemConfirmationResponseDto;
import com.samsam55.trip.trip.dto.ItineraryItemStatusDto;
import com.samsam55.trip.trip.dto.MyVoteBatchResponseDto;
import com.samsam55.trip.trip.dto.MyVoteItemRequestDto;
import com.samsam55.trip.trip.dto.MyVoteResultDto;
import com.samsam55.trip.trip.dto.VoteStartResponseDto;
import com.samsam55.trip.trip.entity.ItineraryItem;
import com.samsam55.trip.trip.entity.ItineraryItemDecisionType;
import com.samsam55.trip.trip.entity.ItineraryItemStatus;
import com.samsam55.trip.trip.entity.Participant;
import com.samsam55.trip.trip.entity.Vote;
import com.samsam55.trip.trip.entity.VoteOption;
import com.samsam55.trip.trip.exception.TripErrorType;
import com.samsam55.trip.trip.repository.ItineraryItemRepository;
import com.samsam55.trip.trip.repository.ParticipantRepository;
import com.samsam55.trip.trip.repository.VoteOptionRepository;
import com.samsam55.trip.trip.repository.VoteRepository;
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
    private final VoteRepository voteRepository;
    private final ParticipantRepository participantRepository;

    /**
     * 준비 중인 일정 항목들을 한 번에 부모에게 올린다. 목록에 담긴 일정 항목 중
     * 하나라도 조건을 만족하지 못하면 전체가 롤백된다. 결정 방식에 따라 처리가
     * 다르다 — VOTE는 부모 투표를 받을 수 있도록 VOTING으로 전이하고, HOST_PICK은
     * 투표 단계를 거치지 않으므로 보유한 선택지 하나로 즉시 CONFIRMED까지 전이한다.
     *
     * @param loginUserId 요청한 회원의 식별자
     * @param itemIds 올릴 일정 항목 식별자 목록
     * @return 변경된 일정 항목들의 상태 목록
     * @throws ApplicationException 일정 항목을 찾을 수 없을 때(ITINERARY_ITEM_NOT_FOUND)
     * @throws ApplicationException 요청자가 여행 방장이 아닐 때(NOT_TRIP_HOST)
     * @throws ApplicationException 이미 투표가 시작됐거나 확정된 일정일 때(ITINERARY_ITEM_ALREADY_OPENED)
     * @throws ApplicationException VOTE 항목의 선택지가 2개 미만일 때(VOTE_OPTION_COUNT_INSUFFICIENT)
     * @throws ApplicationException HOST_PICK 항목에 등록된 선택지(장소)가 없을 때(HOST_PICK_OPTION_REQUIRED)
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
            if (itineraryItem.getStatus() != ItineraryItemStatus.PENDING) {
                throw withItemId(TripErrorType.ITINERARY_ITEM_ALREADY_OPENED, itemId);
            }

            if (itineraryItem.getDecisionType() == ItineraryItemDecisionType.HOST_PICK) {
                VoteOption option = voteOptionRepository.findByItineraryItem(itineraryItem).stream()
                        .findFirst()
                        .orElseThrow(() -> withItemId(TripErrorType.HOST_PICK_OPTION_REQUIRED, itemId));
                itineraryItem.confirm(option);
            } else {
                if (voteOptionRepository.countByItineraryItemId(itemId) < MIN_VOTE_OPTION_COUNT) {
                    throw withItemId(TripErrorType.VOTE_OPTION_COUNT_INSUFFICIENT, itemId);
                }
                itineraryItem.openVote();
            }

            results.add(ItineraryItemStatusDto.from(itineraryItem));
        }
        return VoteStartResponseDto.from(results);
    }

    private ApplicationException withItemId(TripErrorType errorType, Long itemId) {
        return new ApplicationException(errorType, errorType.getMessage() + " (itemId: " + itemId + ")");
    }

    /**
     * 참여자가 여러 일정 항목에 한 번에 투표하거나 기존 투표를 변경한다.
     * 목록에 담긴 일정 항목 중 하나라도 조건을 만족하지 못하면 전체가 롤백된다.
     * 각 일정 항목에 트립의 모든 참여자가 투표를 마치면 상태를 VOTED로 전환한다.
     *
     * @param principal 로그인한 참여자 정보
     * @param voteItems 투표할 일정 항목과 선택할 옵션 목록
     * @return 저장된 투표 정보 목록과 다음으로 투표할 일정 항목의 식별자
     * @throws ApplicationException 일정 항목을 찾을 수 없을 때(ITINERARY_ITEM_NOT_FOUND)
     * @throws ApplicationException 참여자가 이 일정이 속한 여행의 참여자가 아닐 때(TRIP_PARTICIPANT_MISMATCH)
     * @throws ApplicationException 투표 중이거나 투표가 끝난 상태가 아닐 때(ITINERARY_ITEM_NOT_VOTABLE)
     * @throws ApplicationException 옵션이 해당 일정 항목의 선택지가 아닐 때(VOTE_OPTION_NOT_FOUND)
     * @throws ApplicationException 참여자를 찾을 수 없을 때(PARTICIPANT_NOT_FOUND)
     */
    @Transactional
    public MyVoteBatchResponseDto castVotes(ParticipantPrincipal principal, List<MyVoteItemRequestDto> voteItems) {
        Participant participant = participantRepository.findById(principal.participantId())
                .orElseThrow(() -> new ApplicationException(TripErrorType.PARTICIPANT_NOT_FOUND));

        List<MyVoteResultDto> results = new ArrayList<>();
        for (MyVoteItemRequestDto voteItem : voteItems) {
            Long itemId = voteItem.itemId();
            Long voteOptionId = voteItem.voteOptionId();

            ItineraryItem itineraryItem = itineraryItemRepository.findById(itemId)
                    .orElseThrow(() -> withItemId(TripErrorType.ITINERARY_ITEM_NOT_FOUND, itemId));

            if (!itineraryItem.getTripDay().getTrip().getId().equals(principal.tripId())) {
                throw withItemId(TripErrorType.TRIP_PARTICIPANT_MISMATCH, itemId);
            }
            if (itineraryItem.getStatus() != ItineraryItemStatus.VOTING
                    && itineraryItem.getStatus() != ItineraryItemStatus.VOTED) {
                throw withItemId(TripErrorType.ITINERARY_ITEM_NOT_VOTABLE, itemId);
            }

            VoteOption voteOption = voteOptionRepository.findByIdAndItineraryItemId(voteOptionId, itemId)
                    .orElseThrow(() -> withItemId(TripErrorType.VOTE_OPTION_NOT_FOUND, itemId));

            voteRepository.findByItineraryItemIdAndParticipantId(itemId, principal.participantId())
                    .ifPresentOrElse(
                            vote -> vote.changeOption(voteOption),
                            () -> voteRepository.save(new Vote(voteOption, itineraryItem, participant))
                    );

            if (itineraryItem.getStatus() == ItineraryItemStatus.VOTING) {
                long totalParticipantCount = participantRepository.countByTripId(principal.tripId());
                long votedParticipantCount = voteRepository.countByItineraryItemId(itemId);
                if (votedParticipantCount >= totalParticipantCount) {
                    itineraryItem.markVoted();
                }
            }

            results.add(new MyVoteResultDto(itemId, voteOptionId));
        }

        Long nextItemId = itineraryItemRepository
                .findUnvotedVotingItemsOrderByDayAndSortOrder(principal.tripId(), principal.participantId())
                .stream()
                .findFirst()
                .map(ItineraryItem::getId)
                .orElse(null);

        return new MyVoteBatchResponseDto(results, nextItemId);
    }

    /**
     * 여행 방장이 선택지를 직접 골라 일정 항목을 확정한다. 최다 득표 선택지가 아니어도
     * 방장이 임의로 고를 수 있고, 전원 투표가 끝나지 않은 VOTING 상태에서도 확정할 수 있다.
     * decisionType이 HOST_PICK인 일정 항목은 투표를 거치지 않으므로, PENDING 상태에서도
     * (선택지를 추가한 뒤) 바로 확정할 수 있다.
     *
     * @param loginUserId 요청한 회원의 식별자
     * @param itemId 확정할 일정 항목의 식별자
     * @param voteOptionId 확정할 선택지의 식별자
     * @return 확정된 일정 항목의 상태
     * @throws ApplicationException 일정 항목을 찾을 수 없을 때(ITINERARY_ITEM_NOT_FOUND)
     * @throws ApplicationException 요청자가 여행 방장이 아닐 때(NOT_TRIP_HOST)
     * @throws ApplicationException 확정할 수 없는 상태일 때(ITINERARY_ITEM_NOT_VOTABLE) —
     *         VOTE 타입은 VOTING/VOTED 상태여야 하고, HOST_PICK 타입은 PENDING 상태여야 한다
     * @throws ApplicationException 옵션이 해당 일정 항목의 선택지가 아닐 때(VOTE_OPTION_NOT_FOUND)
     */
    @Transactional
    public ItineraryItemConfirmationResponseDto confirm(Long loginUserId, Long itemId, Long voteOptionId) {
        ItineraryItem itineraryItem = itineraryItemRepository.findById(itemId)
                .orElseThrow(() -> new ApplicationException(TripErrorType.ITINERARY_ITEM_NOT_FOUND));

        if (!itineraryItem.getTripDay().getTrip().getHostUser().getId().equals(loginUserId)) {
            throw new ApplicationException(TripErrorType.NOT_TRIP_HOST);
        }
        boolean votable = itineraryItem.getStatus() == ItineraryItemStatus.VOTING
                || itineraryItem.getStatus() == ItineraryItemStatus.VOTED
                || (itineraryItem.getStatus() == ItineraryItemStatus.PENDING
                        && itineraryItem.getDecisionType() == ItineraryItemDecisionType.HOST_PICK);
        if (!votable) {
            throw new ApplicationException(TripErrorType.ITINERARY_ITEM_NOT_VOTABLE);
        }

        VoteOption voteOption = voteOptionRepository.findByIdAndItineraryItemId(voteOptionId, itemId)
                .orElseThrow(() -> new ApplicationException(TripErrorType.VOTE_OPTION_NOT_FOUND));

        itineraryItem.confirm(voteOption);

        return ItineraryItemConfirmationResponseDto.from(itineraryItem);
    }

    /**
     * 확정된 일정 항목의 확정을 해제한다. VOTE 항목은 다시 투표를 받을 수 있도록
     * VOTING 상태로 되돌리며, 기존에 쌓인 투표는 지우지 않고 그대로 보존한다 —
     * 되돌린 뒤 다시 확정할 때 그 투표 결과를 그대로 활용할 수 있다. HOST_PICK
     * 항목은 방장이 확정하기를 눌러야만 확정되는 방식이라 투표를 거친 적이
     * 없으므로, 처음 만들었을 때와 같은 PENDING 상태로 되돌린다. 기존 선택지는
     * 유지해 같은 장소를 다시 확정하거나, PENDING 상태에서 수정한 뒤 재확정할 수
     * 있게 한다.
     *
     * @param loginUserId 요청한 회원의 식별자
     * @param itemId 확정을 해제할 일정 항목의 식별자
     * @return 되돌려진 일정 항목의 상태
     * @throws ApplicationException 일정 항목을 찾을 수 없을 때(ITINERARY_ITEM_NOT_FOUND)
     * @throws ApplicationException 요청자가 여행 방장이 아닐 때(NOT_TRIP_HOST)
     * @throws ApplicationException 확정된 일정이 아닐 때(ITINERARY_ITEM_NOT_CONFIRMED)
     */
    @Transactional
    public ItineraryItemStatusDto unconfirm(Long loginUserId, Long itemId) {
        ItineraryItem itineraryItem = itineraryItemRepository.findById(itemId)
                .orElseThrow(() -> new ApplicationException(TripErrorType.ITINERARY_ITEM_NOT_FOUND));

        if (!itineraryItem.getTripDay().getTrip().getHostUser().getId().equals(loginUserId)) {
            throw new ApplicationException(TripErrorType.NOT_TRIP_HOST);
        }
        if (itineraryItem.getStatus() != ItineraryItemStatus.CONFIRMED) {
            throw new ApplicationException(TripErrorType.ITINERARY_ITEM_NOT_CONFIRMED);
        }

        itineraryItem.unconfirm();

        return ItineraryItemStatusDto.from(itineraryItem);
    }
}
