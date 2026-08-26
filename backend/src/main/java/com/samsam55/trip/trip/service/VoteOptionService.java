package com.samsam55.trip.trip.service;

import com.samsam55.trip.auth.dto.AuthMeResponseDto;
import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.global.exception.GlobalErrorType;
import com.samsam55.trip.trip.ai.VoteOptionDescriptionGenerator;
import com.samsam55.trip.trip.dto.VoteOptionCreateResponseDto;
import com.samsam55.trip.trip.dto.VoteOptionImageDto;
import com.samsam55.trip.trip.dto.VoteOptionSummaryDto;
import com.samsam55.trip.trip.entity.ItineraryItem;
import com.samsam55.trip.trip.entity.ItineraryItemDecisionType;
import com.samsam55.trip.trip.entity.ItineraryItemStatus;
import com.samsam55.trip.trip.entity.Trip;
import com.samsam55.trip.trip.entity.VoteOption;
import com.samsam55.trip.trip.exception.TripErrorType;
import com.samsam55.trip.trip.repository.ItineraryItemRepository;
import com.samsam55.trip.trip.repository.ParticipantRepository;
import com.samsam55.trip.trip.repository.VoteOptionRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class VoteOptionService {

    private static final int MAX_VOTE_OPTION_COUNT = 4;
    private static final String MANUAL_DESCRIPTION_SOURCE = "HOST";

    private final VoteOptionRepository voteOptionRepository;
    private final ItineraryItemRepository itineraryItemRepository;
    private final VoteOptionDescriptionGenerator descriptionGenerator;
    private final ParticipantRepository participantRepository;

    /**
     * 선택지에 등록된 이미지를 조회한다.
     *
     * @param actor 현재 인증 주체
     * @param voteOptionId 조회할 선택지의 식별자
     * @return 이미지 바이트와 콘텐츠 타입
     * @throws ApplicationException 선택지를 찾을 수 없을 때(VOTE_OPTION_NOT_FOUND)
     * @throws ApplicationException 선택지에 등록된 이미지가 없을 때(VOTE_OPTION_IMAGE_NOT_FOUND)
     */
    @Transactional(readOnly = true)
    public VoteOptionImageDto getImage(AuthMeResponseDto actor, Long voteOptionId) {
        VoteOption voteOption = voteOptionRepository.findByIdWithTrip(voteOptionId)
                .orElseThrow(() -> new ApplicationException(TripErrorType.VOTE_OPTION_NOT_FOUND));
        validateAccess(actor, voteOption);

        if (voteOption.getImage() == null) {
            throw new ApplicationException(TripErrorType.VOTE_OPTION_IMAGE_NOT_FOUND);
        }

        return VoteOptionImageDto.from(voteOption);
    }

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
     * @param image 선택지 이미지(선택)
     * @return 생성된 선택지
     * @throws ApplicationException 일정 항목을 찾을 수 없을 때(ITINERARY_ITEM_NOT_FOUND)
     * @throws ApplicationException 요청자가 여행 방장이 아닐 때(NOT_TRIP_HOST)
     * @throws ApplicationException 이름이 비어 있을 때(INVALID_INPUT_VALUE)
     * @throws ApplicationException 투표가 이미 시작된 일정 항목일 때(VOTE_ALREADY_STARTED)
     * @throws ApplicationException 선택지가 이미 4개일 때(VOTE_OPTION_COUNT_EXCEEDED)
     */
    @Transactional
    public VoteOptionCreateResponseDto createVoteOption(
            Long loginUserId, Long itemId, String name, MultipartFile image) {
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

        boolean hasImage = hasContent(image);
        VoteOption voteOption = voteOptionRepository.save(new VoteOption(
                itineraryItem,
                name,
                descriptionGenerator.generate(name),
                descriptionGenerator.getSource(),
                hasImage ? readBytes(image) : null,
                hasImage ? image.getContentType() : null
        ));

        if (itineraryItem.getDecisionType() == ItineraryItemDecisionType.HOST_PICK) {
            itineraryItem.confirm(voteOption);
        }

        return VoteOptionCreateResponseDto.from(voteOption);
    }

    /**
     * 선택지의 이름·설명·이미지를 수정한다. 이미지를 새로 보내지 않으면 기존 이미지를
     * 그대로 유지한다. 설명은 방장이 직접 쓴 것으로 취급해 descriptionSource를 HOST로
     * 바꾼다.
     *
     * @param loginUserId 요청한 회원의 식별자
     * @param voteOptionId 수정할 선택지의 식별자
     * @param name 선택지 이름
     * @param description 선택지 설명(선택)
     * @param image 새로 첨부할 이미지(선택, 없으면 기존 이미지 유지)
     * @return 수정된 선택지
     * @throws ApplicationException 이름이 비어 있을 때(INVALID_INPUT_VALUE)
     * @throws ApplicationException 선택지를 찾을 수 없을 때(VOTE_OPTION_NOT_FOUND)
     * @throws ApplicationException 요청자가 여행 방장이 아닐 때(NOT_TRIP_HOST)
     * @throws ApplicationException 투표가 이미 시작된 일정 항목의 선택지일 때(VOTE_ALREADY_STARTED)
     */
    @Transactional
    public VoteOptionSummaryDto updateVoteOption(
            Long loginUserId, Long voteOptionId, String name, String description, MultipartFile image) {
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

        boolean hasNewImage = hasContent(image);
        byte[] imageBytes = hasNewImage ? readBytes(image) : voteOption.getImage();
        String imageContentType = hasNewImage ? image.getContentType() : voteOption.getImageContentType();

        voteOption.update(name, description, MANUAL_DESCRIPTION_SOURCE, imageBytes, imageContentType);

        return VoteOptionSummaryDto.from(voteOption);
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

    private void validateAccess(AuthMeResponseDto actor, VoteOption voteOption) {
        Trip trip = voteOption.getItineraryItem().getTripDay().getTrip();
        if ("HOST".equals(actor.actorType())) {
            if (!trip.getHostUser().getId().equals(actor.userId())) {
                throw new ApplicationException(TripErrorType.VOTE_OPTION_NOT_FOUND);
            }
            return;
        }

        if (voteOption.getItineraryItem().getStatus() == ItineraryItemStatus.PENDING
                || !trip.getId().equals(actor.tripId())) {
            throw new ApplicationException(TripErrorType.VOTE_OPTION_NOT_FOUND);
        }
        participantRepository.findByIdAndTrip(actor.participantId(), trip)
                .orElseThrow(() -> new ApplicationException(TripErrorType.VOTE_OPTION_NOT_FOUND));
    }
}
