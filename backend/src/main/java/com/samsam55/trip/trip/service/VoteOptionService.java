package com.samsam55.trip.trip.service;

import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.trip.dto.VoteOptionImageDto;
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
}
