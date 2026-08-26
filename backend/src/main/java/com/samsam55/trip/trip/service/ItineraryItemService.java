package com.samsam55.trip.trip.service;

import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.trip.ai.VoteOptionDescriptionGenerator;
import com.samsam55.trip.trip.dto.ItineraryItemCreateRequestDto;
import com.samsam55.trip.trip.dto.ItineraryItemCreateResponseDto;
import com.samsam55.trip.trip.dto.ItineraryItemDetailResponseDto;
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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
                            descriptionGenerator.getSource(),
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
        if (!trip.getHostUser().getId().equals(loginUserId)) {
            throw new ApplicationException(TripErrorType.NOT_TRIP_HOST);
        }

        List<Participant> participants = participantRepository.findAllByTripOrderById(trip);
        List<Vote> votes = voteRepository.findAllByItineraryItemIdWithOptionAndParticipant(itemId);
        Map<Long, Participant> votedParticipantsById = votes.stream()
                .collect(Collectors.toMap(vote -> vote.getParticipant().getId(), Vote::getParticipant, (a, b) -> a));

        List<VoteStatusParticipantResponseDto> participantStatuses = participants.stream()
                .map(participant -> VoteStatusParticipantResponseDto.of(
                        participant, votedParticipantsById.containsKey(participant.getId())))
                .toList();

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

        if (!itineraryItem.getTripDay().getTrip().getHostUser().getId().equals(loginUserId)) {
            throw new ApplicationException(TripErrorType.NOT_TRIP_HOST);
        }

        voteRepository.deleteAllByItineraryItemId(itemId);
        itineraryItemRepository.clearConfirmedOptionByItemId(itemId);
        voteOptionRepository.deleteAllByItineraryItemId(itemId);
        itineraryItemRepository.delete(itineraryItem);
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
