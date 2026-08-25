package com.samsam55.trip.trip.service;

import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.trip.dto.InviteVerifyResponseDto;
import com.samsam55.trip.trip.entity.Participant;
import com.samsam55.trip.trip.entity.Trip;
import com.samsam55.trip.trip.exception.TripErrorType;
import com.samsam55.trip.trip.repository.ParticipantRepository;
import com.samsam55.trip.trip.repository.TripRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InviteService {

    private final TripRepository tripRepository;
    private final ParticipantRepository participantRepository;

    /**
     * 초대 코드로 여행과 참여자 슬롯 선점 현황을 조회한다.
     *
     * @param inviteCode 초대 코드
     * @return 여행 정보와 역할별 선점 여부가 담긴 참여자 슬롯 목록
     * @throws ApplicationException 초대 코드에 해당하는 여행이 없을 때(INVITE_CODE_NOT_FOUND)
     */
    @Transactional(readOnly = true)
    public InviteVerifyResponseDto verify(String inviteCode) {
        Trip trip = tripRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new ApplicationException(TripErrorType.INVITE_CODE_NOT_FOUND));

        List<Participant> participants = participantRepository.findAllByTripOrderById(trip);
        return InviteVerifyResponseDto.of(trip, participants);
    }
}
