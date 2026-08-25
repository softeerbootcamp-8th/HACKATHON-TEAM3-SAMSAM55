package com.samsam55.trip.trip.service;

import com.samsam55.trip.trip.dto.TripListResponseDto;
import com.samsam55.trip.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;

    /**
     * 로그인한 사용자가 방장인 여행 목록을 조회한다.
     *
     * @param userId 여행 목록을 요청한 로그인 사용자의 ID
     * @return 해당 사용자가 방장인 여행 목록
     */
    @Transactional(readOnly = true)
    public TripListResponseDto findTrips(Long userId) {
        return TripListResponseDto.from(tripRepository.findAllByHostUserIdOrderByIdAsc(userId));
    }
}
