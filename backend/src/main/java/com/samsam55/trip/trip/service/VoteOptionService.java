package com.samsam55.trip.trip.service;

import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.global.exception.GlobalErrorType;
import com.samsam55.trip.trip.ai.VoteOptionDescriptionGenerator;
import com.samsam55.trip.trip.dto.VoteOptionCreateResponseDto;
import com.samsam55.trip.trip.dto.VoteOptionSummaryDto;
import com.samsam55.trip.trip.entity.ItineraryItem;
import com.samsam55.trip.trip.entity.ItineraryItemDecisionType;
import com.samsam55.trip.trip.entity.ItineraryItemStatus;
import com.samsam55.trip.trip.entity.VoteOption;
import com.samsam55.trip.trip.exception.TripErrorType;
import com.samsam55.trip.trip.repository.ItineraryItemRepository;
import com.samsam55.trip.trip.repository.VoteOptionRepository;
import com.samsam55.trip.upload.service.S3PresignService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VoteOptionService {

    private static final int MAX_VOTE_OPTION_COUNT = 4;
    private static final String MANUAL_DESCRIPTION_SOURCE = "HOST";

    private final VoteOptionRepository voteOptionRepository;
    private final ItineraryItemRepository itineraryItemRepository;
    private final VoteOptionDescriptionGenerator descriptionGenerator;
    private final S3PresignService s3PresignService;

    /**
     * 선택지를 삭제한다.
     *
     * @param loginUserId 요청한 회원의 식별자
     * @param voteOptionId 삭제할 선택지의 식별자
     * @throws ApplicationException 선택지를 찾을 수 없을 때(VOTE_OPTION_NOT_FOUND)
     * @throws ApplicationException 요청자가 여행 방장이 아닐 때(NOT_TRIP_HOST)
     * @throws ApplicationException 투표가 이미 시작된 일정 항목의 선택지일 때(VOTE_ALREADY_STARTED)
     */
    @Transactional
    public void deleteVoteOption(Long loginUserId, Long voteOptionId) {
        VoteOption voteOption = voteOptionRepository.findById(voteOptionId)
                .orElseThrow(() -> new ApplicationException(TripErrorType.VOTE_OPTION_NOT_FOUND));

        ItineraryItem itineraryItem = voteOption.getItineraryItem();
        if (!itineraryItem.getTripDay().getTrip().getHostUser().getId().equals(loginUserId)) {
            throw new ApplicationException(TripErrorType.NOT_TRIP_HOST);
        }
        if (itineraryItem.getStatus() != ItineraryItemStatus.PENDING) {
            throw new ApplicationException(TripErrorType.VOTE_ALREADY_STARTED);
        }

        voteOptionRepository.delete(voteOption);
    }

    /**
     * 일정 항목에 투표 선택지를 추가한다. {@code decisionType}이 HOST_PICK이면
     * 추가된 선택지가 즉시 확정되어 일정 항목 상태가 CONFIRMED로 전환된다.
     *
     * @param loginUserId 요청한 회원의 식별자
     * @param itemId 선택지를 추가할 일정 항목의 식별자
     * @param name 선택지 이름
     * @param imageKey presigned URL로 미리 업로드한 사진의 S3 key(선택)
     * @return 생성된 선택지
     * @throws ApplicationException 일정 항목을 찾을 수 없을 때(ITINERARY_ITEM_NOT_FOUND)
     * @throws ApplicationException 요청자가 여행 방장이 아닐 때(NOT_TRIP_HOST)
     * @throws ApplicationException 이름이 비어 있을 때(INVALID_INPUT_VALUE)
     * @throws ApplicationException 투표가 이미 시작된 일정 항목일 때(VOTE_ALREADY_STARTED)
     * @throws ApplicationException 선택지가 이미 4개일 때(VOTE_OPTION_COUNT_EXCEEDED)
     */
    @Transactional
    public VoteOptionCreateResponseDto createVoteOption(
            Long loginUserId, Long itemId, String name, String imageKey) {
        if (name == null || name.isBlank()) {
            throw new ApplicationException(GlobalErrorType.INVALID_INPUT_VALUE);
        }

        ItineraryItem itineraryItem = itineraryItemRepository.findById(itemId)
                .orElseThrow(() -> new ApplicationException(TripErrorType.ITINERARY_ITEM_NOT_FOUND));

        if (!itineraryItem.getTripDay().getTrip().getHostUser().getId().equals(loginUserId)) {
            throw new ApplicationException(TripErrorType.NOT_TRIP_HOST);
        }
        if (itineraryItem.getStatus() != ItineraryItemStatus.PENDING) {
            throw new ApplicationException(TripErrorType.VOTE_ALREADY_STARTED);
        }
        if (voteOptionRepository.countByItineraryItem(itineraryItem) >= MAX_VOTE_OPTION_COUNT) {
            throw new ApplicationException(TripErrorType.VOTE_OPTION_COUNT_EXCEEDED);
        }

        VoteOption voteOption = voteOptionRepository.save(new VoteOption(
                itineraryItem,
                name,
                descriptionGenerator.generate(name),
                descriptionGenerator.getSource(),
                imageKey
        ));

        if (itineraryItem.getDecisionType() == ItineraryItemDecisionType.HOST_PICK) {
            itineraryItem.confirm(voteOption);
        }

        return VoteOptionCreateResponseDto.from(voteOption, s3PresignService.toPublicUrl(imageKey));
    }

    /**
     * 선택지의 이름·설명·이미지를 수정한다. imageKey를 새로 보내지 않으면 기존 이미지를
     * 그대로 유지한다. 설명은 방장이 직접 쓴 것으로 취급해 descriptionSource를 HOST로
     * 바꾼다.
     *
     * @param loginUserId 요청한 회원의 식별자
     * @param voteOptionId 수정할 선택지의 식별자
     * @param name 선택지 이름
     * @param description 선택지 설명(선택)
     * @param imageKey presigned URL로 미리 업로드한 새 사진의 S3 key(선택, 없으면 기존 이미지 유지)
     * @return 수정된 선택지
     * @throws ApplicationException 이름이 비어 있을 때(INVALID_INPUT_VALUE)
     * @throws ApplicationException 선택지를 찾을 수 없을 때(VOTE_OPTION_NOT_FOUND)
     * @throws ApplicationException 요청자가 여행 방장이 아닐 때(NOT_TRIP_HOST)
     * @throws ApplicationException 투표가 이미 시작된 일정 항목의 선택지일 때(VOTE_ALREADY_STARTED)
     */
    @Transactional
    public VoteOptionSummaryDto updateVoteOption(
            Long loginUserId, Long voteOptionId, String name, String description, String imageKey) {
        if (name == null || name.isBlank()) {
            throw new ApplicationException(GlobalErrorType.INVALID_INPUT_VALUE);
        }

        VoteOption voteOption = voteOptionRepository.findById(voteOptionId)
                .orElseThrow(() -> new ApplicationException(TripErrorType.VOTE_OPTION_NOT_FOUND));

        ItineraryItem itineraryItem = voteOption.getItineraryItem();
        if (!itineraryItem.getTripDay().getTrip().getHostUser().getId().equals(loginUserId)) {
            throw new ApplicationException(TripErrorType.NOT_TRIP_HOST);
        }
        if (itineraryItem.getStatus() != ItineraryItemStatus.PENDING) {
            throw new ApplicationException(TripErrorType.VOTE_ALREADY_STARTED);
        }

        String resolvedImageKey = imageKey != null ? imageKey : voteOption.getImageKey();
        voteOption.update(name, description, MANUAL_DESCRIPTION_SOURCE, resolvedImageKey);

        return VoteOptionSummaryDto.from(voteOption, s3PresignService.toPublicUrl(resolvedImageKey));
    }
}
