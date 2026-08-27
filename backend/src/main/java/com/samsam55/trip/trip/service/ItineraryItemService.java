package com.samsam55.trip.trip.service;

import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.trip.ai.VoteOptionDescriptionGenerator;
import com.samsam55.trip.trip.dto.ItineraryItemBasicInfoUpdateRequestDto;
import com.samsam55.trip.trip.dto.ItineraryItemCreateRequestDto;
import com.samsam55.trip.trip.dto.ItineraryItemCreateResponseDto;
import com.samsam55.trip.trip.dto.ItineraryItemDetailResponseDto;
import com.samsam55.trip.trip.dto.ItineraryItemUpdateRequestDto;
import com.samsam55.trip.trip.dto.VoteOptionCreateItemDto;
import com.samsam55.trip.trip.dto.VoteOptionSummaryDto;
import com.samsam55.trip.trip.dto.VoteResultParticipantResponseDto;
import com.samsam55.trip.trip.dto.VoteStatusOptionResponseDto;
import com.samsam55.trip.trip.dto.VoteStatusParticipantResponseDto;
import com.samsam55.trip.trip.dto.VoteStatusResponseDto;
import com.samsam55.trip.trip.entity.ItineraryItem;
import com.samsam55.trip.trip.entity.ItineraryItemDecisionType;
import com.samsam55.trip.trip.entity.ItineraryItemStatus;
import com.samsam55.trip.trip.entity.Participant;
import com.samsam55.trip.trip.entity.Trip;
import com.samsam55.trip.trip.entity.TripDay;
import com.samsam55.trip.trip.entity.Vote;
import com.samsam55.trip.trip.entity.VoteOption;
import com.samsam55.trip.trip.exception.TripErrorType;
import com.samsam55.trip.trip.repository.ItineraryItemRepository;
import com.samsam55.trip.trip.repository.ParticipantRepository;
import com.samsam55.trip.trip.repository.TripDayRepository;
import com.samsam55.trip.trip.repository.VoteOptionRepository;
import com.samsam55.trip.trip.repository.VoteRepository;
import com.samsam55.trip.upload.service.S3PresignService;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItineraryItemService {

    private static final int MAX_VOTE_OPTION_COUNT = 4;

    private final TripDayRepository tripDayRepository;
    private final ItineraryItemRepository itineraryItemRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final VoteOptionDescriptionGenerator descriptionGenerator;
    private final ParticipantRepository participantRepository;
    private final VoteRepository voteRepository;
    private final S3PresignService s3PresignService;

    /**
     * 일차에 새 일정 항목을 생성한다. 결정 방식이 VOTE이고 선택지가 있으면
     * 선택지별 AI 설명까지 동기로 생성한 뒤 응답한다.
     *
     * @param loginUserId 요청한 회원의 식별자
     * @param tripDayId 일정 항목을 추가할 일차의 식별자
     * @param request 이름·카테고리·결정 방식·선택지(사진은 미리 업로드한 S3 key로 전달)가 담긴 생성 요청
     * @return 생성된 일정 항목과 선택지 목록
     * @throws ApplicationException 일차를 찾을 수 없을 때(TRIP_DAY_NOT_FOUND)
     * @throws ApplicationException 요청자가 여행 방장이 아닐 때(NOT_TRIP_HOST)
     * @throws ApplicationException VOTE 선택지가 4개를 초과할 때(VOTE_OPTION_COUNT_EXCEEDED)
     */
    @Transactional
    public ItineraryItemCreateResponseDto createItineraryItem(
            Long loginUserId, Long tripDayId, ItineraryItemCreateRequestDto request) {
        // 같은 일차에 동시에 생성 요청이 오면 sortOrder가 겹칠 수 있어 TripDay row를 잠그고 진행
        TripDay tripDay = tripDayRepository.findByIdForUpdate(tripDayId)
                .orElseThrow(() -> new ApplicationException(TripErrorType.TRIP_DAY_NOT_FOUND));

        // 방장 권한 확인
        if (!tripDay.getTrip().getHostUser().getId().equals(loginUserId)) {
            throw new ApplicationException(TripErrorType.NOT_TRIP_HOST);
        }

        // HOST_PICK은 선택지를 무시하고, VOTE는 최대 4개까지만 허용
        ItineraryItemDecisionType decisionType = ItineraryItemDecisionType.valueOf(request.decisionType());
        List<VoteOptionCreateItemDto> options = decisionType == ItineraryItemDecisionType.VOTE
                ? Optional.ofNullable(request.options()).orElseGet(List::of)
                : List.of();
        if (options.size() > MAX_VOTE_OPTION_COUNT) {
            throw new ApplicationException(TripErrorType.VOTE_OPTION_COUNT_EXCEEDED);
        }

        // 일정 항목은 해당 일차의 마지막 순서 다음으로 생성
        int sortOrder = itineraryItemRepository.findMaxSortOrderByTripDayId(tripDayId) + 1;
        ItineraryItem itineraryItem = itineraryItemRepository.save(new ItineraryItem(
                tripDay, request.name(), request.category(), decisionType,
                ItineraryItemStatus.PENDING, sortOrder, null
        ));

        // 선택지별로 AI 설명을 동기 생성하고, 있으면 이미지 key도 함께 저장
        List<VoteOptionSummaryDto> voteOptions = options.stream()
                .map(option -> voteOptionRepository.save(new VoteOption(
                        itineraryItem,
                        option.name(),
                        descriptionGenerator.generate(option.name()),
                        descriptionGenerator.getSource(),
                        option.imageKey()
                )))
                .map(voteOption -> VoteOptionSummaryDto.from(
                        voteOption, s3PresignService.toPublicUrl(voteOption.getImageKey())))
                .toList();

        return ItineraryItemCreateResponseDto.from(itineraryItem, voteOptions);
    }

    /**
     * 일정 항목의 이름·카테고리·결정 방식을 수정한다. 투표가 시작되기 전(PENDING)에만 허용한다 —
     * VOTING 이상에서 decisionType을 바꾸면 이미 쌓인 선택지·투표와 정합이 깨진다.
     * 결정 방식을 VOTE에서 HOST_PICK으로 바꾸는데 기존 선택지가 2개 이상이면,
     * {@code request.selectedOptionId()}로 남길 선택지 하나를 반드시 골라야 하고
     * 나머지 선택지는 삭제된다. HOST_PICK에서 VOTE로 바꾸거나, 기존 선택지가 0~1개면
     * 별도 처리 없이 그대로 수정된다. 이 메서드는 일정을 확정(CONFIRMED)하지 않는다 —
     * 확정은 별도 API의 책임이다.
     *
     * @param loginUserId 요청한 회원의 식별자
     * @param itemId 수정할 일정 항목의 식별자
     * @param request 이름·카테고리·결정 방식·(선택) 유지할 선택지 식별자가 담긴 수정 요청
     * @return 수정된 일정 항목과 선택지 목록
     * @throws ApplicationException 일정 항목을 찾을 수 없을 때(ITINERARY_ITEM_NOT_FOUND)
     * @throws ApplicationException 요청자가 여행 방장이 아닐 때(NOT_TRIP_HOST)
     * @throws ApplicationException 투표가 이미 시작된 일정일 때(VOTE_ALREADY_STARTED)
     * @throws ApplicationException VOTE에서 HOST_PICK으로 바꾸는데 기존 선택지가 2개 이상이면서
     *         {@code selectedOptionId}가 없을 때(VOTE_OPTION_SELECTION_REQUIRED)
     * @throws ApplicationException {@code selectedOptionId}가 이 일정 항목의 선택지가 아닐 때(VOTE_OPTION_NOT_FOUND)
     */
    @Transactional
    public ItineraryItemDetailResponseDto updateItineraryItem(
            Long loginUserId, Long itemId, ItineraryItemUpdateRequestDto request) {
        ItineraryItem itineraryItem = itineraryItemRepository.findById(itemId)
                .orElseThrow(() -> new ApplicationException(TripErrorType.ITINERARY_ITEM_NOT_FOUND));

        // 방장 권한 확인
        if (!itineraryItem.getTripDay().getTrip().getHostUser().getId().equals(loginUserId)) {
            throw new ApplicationException(TripErrorType.NOT_TRIP_HOST);
        }
        // 투표 시작 전(PENDING)에만 수정 허용
        if (itineraryItem.getStatus() != ItineraryItemStatus.PENDING) {
            throw new ApplicationException(TripErrorType.VOTE_ALREADY_STARTED);
        }

        ItineraryItemDecisionType oldDecisionType = itineraryItem.getDecisionType();
        ItineraryItemDecisionType newDecisionType = ItineraryItemDecisionType.valueOf(request.decisionType());
        List<VoteOption> existingOptions = voteOptionRepository.findByItineraryItem(itineraryItem);

        // VOTE에서 HOST_PICK으로 바꾸는데 선택지가 2개 이상이면, 남길 선택지 하나만 빼고 나머지 삭제
        if (oldDecisionType == ItineraryItemDecisionType.VOTE
                && newDecisionType == ItineraryItemDecisionType.HOST_PICK
                && existingOptions.size() >= 2) {
            if (request.selectedOptionId() == null) {
                throw new ApplicationException(TripErrorType.VOTE_OPTION_SELECTION_REQUIRED);
            }
            VoteOption selectedOption = existingOptions.stream()
                    .filter(option -> option.getId().equals(request.selectedOptionId()))
                    .findFirst()
                    .orElseThrow(() -> new ApplicationException(TripErrorType.VOTE_OPTION_NOT_FOUND));

            List<VoteOption> optionsToRemove = existingOptions.stream()
                    .filter(option -> !option.getId().equals(selectedOption.getId()))
                    .toList();
            voteOptionRepository.deleteAll(optionsToRemove);
        }

        // 이름·카테고리·결정 방식 반영
        itineraryItem.update(request.name(), request.category(), newDecisionType);

        List<VoteOptionSummaryDto> voteOptions = voteOptionRepository.findByItineraryItem(itineraryItem).stream()
                .map(voteOption -> VoteOptionSummaryDto.from(
                        voteOption, s3PresignService.toPublicUrl(voteOption.getImageKey())))
                .toList();
        return ItineraryItemDetailResponseDto.from(itineraryItem, voteOptions);
    }

    /**
     * 일정 항목의 이름·카테고리만 수정한다. 결정 방식·선택지와는 무관해 상태와
     * 관계없이(PENDING 이후에도) 허용한다. 결정 방식을 바꾸려면 {@link #updateItineraryItem}을
     * 쓴다.
     *
     * <p>엔티티를 로드해 필드를 바꾼 뒤 저장하지 않고 name/category만 부분 UPDATE로
     * 반영한다 — 이 일정이 VOTING/VOTED인 동안 다른 트랜잭션이 status·confirmedOption을
     * 바꿀 수 있는데(전원 투표 완료, 방장 확정), 엔티티 저장 방식은 그 변경을 로드해 둔
     * 옛 값으로 덮어써버린다.
     *
     * @param loginUserId 요청한 회원의 식별자
     * @param itemId 수정할 일정 항목의 식별자
     * @param request 이름·카테고리가 담긴 수정 요청
     * @return 수정된 일정 항목과 선택지 목록
     * @throws ApplicationException 일정 항목을 찾을 수 없을 때(ITINERARY_ITEM_NOT_FOUND)
     * @throws ApplicationException 요청자가 여행 방장이 아닐 때(NOT_TRIP_HOST)
     */
    @Transactional
    public ItineraryItemDetailResponseDto updateBasicInfo(
            Long loginUserId, Long itemId, ItineraryItemBasicInfoUpdateRequestDto request) {
        ItineraryItem itineraryItem = itineraryItemRepository.findById(itemId)
                .orElseThrow(() -> new ApplicationException(TripErrorType.ITINERARY_ITEM_NOT_FOUND));

        // 방장 권한 확인
        if (!itineraryItem.getTripDay().getTrip().getHostUser().getId().equals(loginUserId)) {
            throw new ApplicationException(TripErrorType.NOT_TRIP_HOST);
        }

        // name/category만 부분 UPDATE (엔티티 저장 방식이면 동시에 바뀐 status 등을 덮어쓸 수 있음)
        itineraryItemRepository.updateBasicInfo(itemId, request.name(), request.category());

        ItineraryItem updated = itineraryItemRepository.findById(itemId)
                .orElseThrow(() -> new ApplicationException(TripErrorType.ITINERARY_ITEM_NOT_FOUND));
        List<VoteOptionSummaryDto> voteOptions = voteOptionRepository.findByItineraryItem(updated).stream()
                .map(voteOption -> VoteOptionSummaryDto.from(
                        voteOption, s3PresignService.toPublicUrl(voteOption.getImageKey())))
                .toList();
        return ItineraryItemDetailResponseDto.from(updated, voteOptions);
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

        // 방장 권한 확인
        if (!itineraryItem.getTripDay().getTrip().getHostUser().getId().equals(loginUserId)) {
            throw new ApplicationException(TripErrorType.NOT_TRIP_HOST);
        }

        List<VoteOptionSummaryDto> voteOptions = voteOptionRepository.findByItineraryItem(itineraryItem).stream()
                .map(voteOption -> VoteOptionSummaryDto.from(
                        voteOption, s3PresignService.toPublicUrl(voteOption.getImageKey())))
                .toList();

        return ItineraryItemDetailResponseDto.from(itineraryItem, voteOptions);
    }

    /**
     * 일정 항목의 투표 진행 현황(참여자별 투표 여부, 선택지별 득표수)을 조회한다.
     *
     * @param loginUserId 요청한 회원의 식별자
     * @param itemId 조회할 일정 항목의 식별자
     * @return 참여자별 투표 여부와 선택지별 득표 현황
     * @throws ApplicationException 일정 항목을 찾을 수 없을 때(ITINERARY_ITEM_NOT_FOUND)
     * @throws ApplicationException 요청자가 여행 방장이 아닐 때(NOT_TRIP_HOST)
     */
    @Transactional(readOnly = true)
    public VoteStatusResponseDto getVoteStatus(Long loginUserId, Long itemId) {
        ItineraryItem itineraryItem = itineraryItemRepository.findById(itemId)
                .orElseThrow(() -> new ApplicationException(TripErrorType.ITINERARY_ITEM_NOT_FOUND));

        Trip trip = itineraryItem.getTripDay().getTrip();
        // 방장 권한 확인
        if (!trip.getHostUser().getId().equals(loginUserId)) {
            throw new ApplicationException(TripErrorType.NOT_TRIP_HOST);
        }

        // 전체 참여자 목록과 이 일정의 투표 기록 조회
        List<Participant> participants = participantRepository.findAllByTripOrderById(trip);
        List<Vote> votes = voteRepository.findAllByItineraryItemIdWithOptionAndParticipant(itemId);
        Map<Long, Participant> votedParticipantsById = votes.stream()
                .collect(Collectors.toMap(vote -> vote.getParticipant().getId(), Vote::getParticipant, (a, b) -> a));

        // 참여자별 투표 완료 여부 계산
        List<VoteStatusParticipantResponseDto> participantStatuses = participants.stream()
                .map(participant -> VoteStatusParticipantResponseDto.of(
                        participant, votedParticipantsById.containsKey(participant.getId())))
                .toList();

        // 선택지별 득표자 집계
        List<VoteOption> options = voteOptionRepository.findByItineraryItem(itineraryItem);
        Map<Long, List<VoteResultParticipantResponseDto>> votersByOptionId = votes.stream()
                .collect(Collectors.groupingBy(
                        vote -> vote.getOption().getId(),
                        Collectors.mapping(
                                vote -> VoteResultParticipantResponseDto.from(vote.getParticipant()),
                                Collectors.toList())));
        List<VoteStatusOptionResponseDto> optionStatuses = options.stream()
                .map(option -> VoteStatusOptionResponseDto.of(
                        option, votersByOptionId.getOrDefault(option.getId(), List.of())))
                .toList();

        return new VoteStatusResponseDto(
                votedParticipantsById.size(), participants.size(), participantStatuses, optionStatuses);
    }

    /**
     * 일정 항목을 삭제한다. 투표 기록·선택지도 함께 삭제되며, 상태와 무관하게(투표 중이거나
     * 확정된 항목도) 삭제할 수 있다.
     *
     * @param loginUserId 요청한 회원의 식별자
     * @param itemId 삭제할 일정 항목의 식별자
     * @throws ApplicationException 일정 항목을 찾을 수 없을 때(ITINERARY_ITEM_NOT_FOUND)
     * @throws ApplicationException 요청자가 여행 방장이 아닐 때(NOT_TRIP_HOST)
     */
    @Transactional
    public void deleteItineraryItem(Long loginUserId, Long itemId) {
        ItineraryItem itineraryItem = itineraryItemRepository.findByIdWithTripAndConfirmedOption(itemId)
                .orElseThrow(() -> new ApplicationException(TripErrorType.ITINERARY_ITEM_NOT_FOUND));

        // 방장 권한 확인
        if (!itineraryItem.getTripDay().getTrip().getHostUser().getId().equals(loginUserId)) {
            throw new ApplicationException(TripErrorType.NOT_TRIP_HOST);
        }

        // 투표 기록 -> 확정 선택지 참조 해제 -> 선택지 -> 일정 항목 순서로 삭제
        voteRepository.deleteAllByItineraryItemId(itemId);
        itineraryItemRepository.clearConfirmedOptionByItemId(itemId);
        // 위 삭제가 영속성 컨텍스트를 비우면서 itineraryItem은 detached 상태가 되므로,
        // 그 객체를 그대로 지우지 않고 id로 새로 조회해서 지운다.
        voteOptionRepository.deleteAllByItineraryItemId(itemId);
        itineraryItemRepository.deleteById(itemId);
    }

    /**
     * 같은 일차 안에서 일정 항목의 순서를 바꾼다. {@code itemIds}는 그 일차에 있는 모든 일정 항목의
     * 식별자를 새 순서대로 나열한 것이어야 한다 — 하나라도 빠지거나 다른 일차의 항목이 섞이면 거부한다.
     *
     * @param loginUserId 요청한 회원의 식별자
     * @param tripDayId 순서를 바꿀 일차의 식별자
     * @param itemIds 새 순서대로 나열한 일정 항목 식별자 목록
     * @throws ApplicationException 일차를 찾을 수 없을 때(TRIP_DAY_NOT_FOUND)
     * @throws ApplicationException 요청자가 여행 방장이 아닐 때(NOT_TRIP_HOST)
     * @throws ApplicationException itemIds가 그 일차의 일정 항목 목록과 정확히 일치하지 않을 때(ITINERARY_ITEM_ORDER_MISMATCH)
     */
    @Transactional
    public void reorderItineraryItems(Long loginUserId, Long tripDayId, List<Long> itemIds) {
        TripDay tripDay = tripDayRepository.findByIdForUpdate(tripDayId)
                .orElseThrow(() -> new ApplicationException(TripErrorType.TRIP_DAY_NOT_FOUND));

        // 방장 권한 확인
        if (!tripDay.getTrip().getHostUser().getId().equals(loginUserId)) {
            throw new ApplicationException(TripErrorType.NOT_TRIP_HOST);
        }

        // 요청받은 itemIds가 이 일차의 전체 항목과 정확히 일치하는지 검증
        List<ItineraryItem> currentItems = itineraryItemRepository.findByTripDayIdOrderBySortOrderAsc(tripDayId);
        Set<Long> currentItemIds = currentItems.stream().map(ItineraryItem::getId).collect(Collectors.toSet());
        if (currentItemIds.size() != itemIds.size() || !currentItemIds.equals(new HashSet<>(itemIds))) {
            throw new ApplicationException(TripErrorType.ITINERARY_ITEM_ORDER_MISMATCH);
        }

        // (trip_day_id, sort_order) 유니크 제약 때문에 최종 순서를 바로 적용하면 중간에 다른 항목과
        // 값이 겹칠 수 있다. 먼저 전부 겹칠 일 없는 임시 음수 값으로 밀어둔 뒤, 최종 순서로 다시 채운다.
        for (int i = 0; i < itemIds.size(); i++) {
            itineraryItemRepository.updateSortOrder(itemIds.get(i), -(i + 1));
        }
        for (int i = 0; i < itemIds.size(); i++) {
            itineraryItemRepository.updateSortOrder(itemIds.get(i), i + 1);
        }
    }
}
