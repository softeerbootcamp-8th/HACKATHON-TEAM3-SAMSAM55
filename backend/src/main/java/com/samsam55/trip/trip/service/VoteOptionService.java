package com.samsam55.trip.trip.service;

import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.trip.dto.VoteOptionImageDto;
import com.samsam55.trip.trip.entity.ItineraryItem;
import com.samsam55.trip.trip.entity.ItineraryItemStatus;
import com.samsam55.trip.trip.entity.VoteOption;
import com.samsam55.trip.trip.exception.TripErrorType;
import com.samsam55.trip.trip.repository.VoteOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VoteOptionService {

    private final VoteOptionRepository voteOptionRepository;

    /**
     * 선택지에 등록된 이미지를 조회한다.
     *
     * @param voteOptionId 조회할 선택지의 식별자
     * @return 이미지 바이트와 콘텐츠 타입
     * @throws ApplicationException 선택지를 찾을 수 없을 때(VOTE_OPTION_NOT_FOUND)
     * @throws ApplicationException 선택지에 등록된 이미지가 없을 때(VOTE_OPTION_IMAGE_NOT_FOUND)
     */
    @Transactional(readOnly = true)
    public VoteOptionImageDto getImage(Long voteOptionId) {
        VoteOption voteOption = voteOptionRepository.findById(voteOptionId)
                .orElseThrow(() -> new ApplicationException(TripErrorType.VOTE_OPTION_NOT_FOUND));

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
}
