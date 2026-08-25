package com.samsam55.trip.trip.service;

import com.samsam55.trip.auth.dto.ActorPrincipal;
import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.trip.dto.VoteOptionImageDto;
import com.samsam55.trip.trip.entity.ItineraryItemStatus;
import com.samsam55.trip.trip.entity.Trip;
import com.samsam55.trip.trip.entity.VoteOption;
import com.samsam55.trip.trip.exception.TripErrorType;
import com.samsam55.trip.trip.repository.ParticipantRepository;
import com.samsam55.trip.trip.repository.VoteOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VoteOptionService {

    private final VoteOptionRepository voteOptionRepository;
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
    public VoteOptionImageDto getImage(ActorPrincipal actor, Long voteOptionId) {
        VoteOption voteOption = voteOptionRepository.findByIdWithTrip(voteOptionId)
                .orElseThrow(() -> new ApplicationException(TripErrorType.VOTE_OPTION_NOT_FOUND));
        validateAccess(actor, voteOption);

        if (voteOption.getImage() == null) {
            throw new ApplicationException(TripErrorType.VOTE_OPTION_IMAGE_NOT_FOUND);
        }

        return VoteOptionImageDto.from(voteOption);
    }

    private void validateAccess(ActorPrincipal actor, VoteOption voteOption) {
        Trip trip = voteOption.getItineraryItem().getTripDay().getTrip();
        if (actor.actorType() == ActorPrincipal.ActorType.HOST) {
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
