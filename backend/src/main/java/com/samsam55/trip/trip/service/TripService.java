package com.samsam55.trip.trip.service;

import com.samsam55.trip.auth.exception.AuthErrorType;
import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.member.entity.User;
import com.samsam55.trip.member.repository.UserRepository;
import com.samsam55.trip.trip.dto.TripCreateRequestDto;
import com.samsam55.trip.trip.dto.TripCreateResponseDto;
import com.samsam55.trip.trip.dto.TripDetailResponseDto;
import com.samsam55.trip.trip.dto.TripListResponseDto;
import com.samsam55.trip.trip.dto.TripSummaryResponseDto;
import com.samsam55.trip.trip.dto.TripUpdateRequestDto;
import com.samsam55.trip.trip.entity.ItineraryItem;
import com.samsam55.trip.trip.entity.Participant;
import com.samsam55.trip.trip.entity.Trip;
import com.samsam55.trip.trip.entity.TripDay;
import com.samsam55.trip.trip.exception.TripErrorType;
import com.samsam55.trip.trip.repository.TripRepository;
import com.samsam55.trip.trip.repository.ItineraryItemRepository;
import com.samsam55.trip.trip.repository.ParticipantRepository;
import com.samsam55.trip.trip.repository.TripDayRepository;
import com.samsam55.trip.trip.repository.VoteOptionRepository;
import com.samsam55.trip.trip.repository.VoteRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final TripDayRepository tripDayRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final ItineraryItemRepository itineraryItemRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final VoteRepository voteRepository;

    /**
     * 로그인한 사용자가 방장인 여행 목록을 조회한다.
     *
     * @param userId 여행 목록을 요청한 로그인 사용자의 ID
     * @return 해당 사용자가 방장인 여행 목록
     */
    @Transactional(readOnly = true)
    public TripListResponseDto findTrips(Long userId) {
        List<Trip> trips = tripRepository.findAllByHostUserIdOrderByStartDateAscIdAsc(userId);
        return TripListResponseDto.from(orderTripsForList(trips));
    }

    /**
     * 로그인한 사용자가 방장인 여행의 상세 정보를 조회한다.
     *
     * @param userId 여행 상세를 요청한 로그인 사용자의 ID
     * @param tripId 조회할 여행의 ID
     * @return 여행 기본 정보와 날짜별 일정 목록
     * @throws ApplicationException 여행이 없거나 방장이 아닐 때(TRIP_NOT_FOUND)
     */
    @Transactional(readOnly = true)
    public TripDetailResponseDto findTrip(Long userId, Long tripId) {
        Trip trip = tripRepository.findByIdAndHostUserId(tripId, userId)
                .orElseThrow(() -> new ApplicationException(TripErrorType.TRIP_NOT_FOUND));
        List<TripDay> tripDays = tripDayRepository.findAllByTripIdOrderByDayNumberAsc(tripId);
        List<ItineraryItem> itineraryItems = itineraryItemRepository
                .findAllByTripIdOrderByDayAndSortOrder(tripId);
        return TripDetailResponseDto.from(trip, tripDays, itineraryItems);
    }

    /**
     * 로그인한 사용자를 방장으로 여행과 여행 일차, 참여자 빈 슬롯을 생성한다.
     *
     * @param userId 여행을 생성하는 로그인 사용자의 ID
     * @param request 여행 제목, 기간, 동행자 역할 목록
     * @return 생성된 여행과 참여자 빈 슬롯
     * @throws ApplicationException 세션의 사용자를 찾을 수 없을 때(LOGIN_REQUIRED)
     * @throws ApplicationException 시작일이 종료일보다 늦을 때(INVALID_TRIP_PERIOD)
     */
    @Transactional
    public TripCreateResponseDto createTrip(Long userId, TripCreateRequestDto request) {
        validateTripPeriod(request.startDate(), request.endDate());

        User hostUser = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(AuthErrorType.LOGIN_REQUIRED));
        LocalDateTime startDate = request.startDate().atStartOfDay();
        LocalDateTime endDate = request.endDate().atStartOfDay();
        Trip trip = tripRepository.saveAndFlush(new Trip(
                hostUser,
                request.title(),
                startDate,
                endDate,
                request.companions().size(),
                createInviteCode()
        ));

        tripDayRepository.saveAll(createTripDays(trip, request.startDate(), request.endDate()));
        List<Participant> participants = request.companions().stream()
                .map(roleName -> new Participant(trip, roleName, null))
                .toList();
        List<Participant> savedParticipants = participantRepository.saveAllAndFlush(participants);
        return TripCreateResponseDto.from(trip, savedParticipants);
    }

    /**
     * 방장이 소유한 여행 정보를 전체 교체하고 여행 일차를 새 기간에 맞춘다.
     *
     * @param userId 수정 요청을 한 로그인 사용자의 ID
     * @param tripId 수정할 여행의 ID
     * @param request 여행 제목과 새 여행 기간
     * @return 수정된 여행 요약 정보
     * @throws ApplicationException 여행이 없거나 방장이 아닐 때(TRIP_NOT_FOUND)
     * @throws ApplicationException 시작일이 종료일보다 늦을 때(INVALID_TRIP_PERIOD)
     */
    @Transactional
    public TripSummaryResponseDto updateTrip(Long userId, Long tripId, TripUpdateRequestDto request) {
        validateTripPeriod(request.startDate(), request.endDate());
        Trip trip = tripRepository.findByIdAndHostUserId(tripId, userId)
                .orElseThrow(() -> new ApplicationException(TripErrorType.TRIP_NOT_FOUND));

        synchronizeTripDays(trip, request.startDate(), request.endDate());
        trip.update(
                request.title(),
                request.startDate().atStartOfDay(),
                request.endDate().atStartOfDay()
        );
        return TripSummaryResponseDto.from(trip);
    }

    /**
     * 방장이 소유한 여행과 모든 하위 데이터를 하나의 트랜잭션으로 삭제한다.
     *
     * @param userId 삭제를 요청한 로그인 사용자의 ID
     * @param tripId 삭제할 여행의 ID
     * @throws ApplicationException 여행이 없거나 방장이 아닐 때(TRIP_NOT_FOUND)
     */
    @Transactional
    public void deleteTrip(Long userId, Long tripId) {
        Trip trip = tripRepository.findByIdAndHostUserId(tripId, userId)
                .orElseThrow(() -> new ApplicationException(TripErrorType.TRIP_NOT_FOUND));

        voteRepository.deleteAllByTripId(tripId);
        itineraryItemRepository.clearConfirmedOptionByTripId(tripId);
        voteOptionRepository.deleteAllByTripId(tripId);
        itineraryItemRepository.deleteAllByTripId(tripId);
        participantRepository.deleteAllByTripId(tripId);
        tripDayRepository.deleteAllByTripId(tripId);
        tripRepository.delete(trip);
        tripRepository.flush();
    }

    private void validateTripPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ApplicationException(TripErrorType.INVALID_TRIP_PERIOD);
        }
    }

    private void synchronizeTripDays(Trip trip, LocalDate startDate, LocalDate endDate) {
        List<TripDay> existingTripDays = tripDayRepository.findAllByTripIdOrderByDayNumberAsc(trip.getId());
        Map<LocalDate, TripDay> tripDaysByDate = new HashMap<>();
        existingTripDays.forEach(tripDay -> tripDaysByDate.put(tripDay.getTripDate(), tripDay));

        List<TripDay> removedTripDays = existingTripDays.stream()
                .filter(tripDay -> tripDay.getTripDate().isBefore(startDate)
                        || tripDay.getTripDate().isAfter(endDate))
                .toList();
        deleteTripDays(removedTripDays);

        List<TripDay> newTripDays = new ArrayList<>();
        long tripLength = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        for (int dayNumber = 1; dayNumber <= tripLength; dayNumber++) {
            LocalDate tripDate = startDate.plusDays(dayNumber - 1L);
            TripDay tripDay = tripDaysByDate.get(tripDate);
            if (tripDay == null) {
                newTripDays.add(new TripDay(trip, dayNumber, tripDate));
                continue;
            }
            tripDay.updateDayNumber(dayNumber);
        }
        if (!newTripDays.isEmpty()) {
            tripDayRepository.saveAll(newTripDays);
        }
    }

    private void deleteTripDays(List<TripDay> tripDays) {
        if (tripDays.isEmpty()) {
            return;
        }
        List<Long> tripDayIds = tripDays.stream()
                .map(TripDay::getId)
                .toList();
        voteRepository.deleteAllByTripDayIds(tripDayIds);
        itineraryItemRepository.clearConfirmedOptionByTripDayIds(tripDayIds);
        voteOptionRepository.deleteAllByTripDayIds(tripDayIds);
        itineraryItemRepository.deleteAllByTripDayIds(tripDayIds);
        tripDayRepository.deleteAll(tripDays);
    }

    private List<Trip> orderTripsForList(List<Trip> trips) {
        LocalDate today = LocalDate.now();
        return Stream.concat(
                trips.stream().filter(trip -> isInProgress(trip, today)),
                trips.stream().filter(trip -> !isInProgress(trip, today))
        ).toList();
    }

    private boolean isInProgress(Trip trip, LocalDate today) {
        return !trip.getEndDate().toLocalDate().isBefore(today);
    }

    private List<TripDay> createTripDays(Trip trip, LocalDate startDate, LocalDate endDate) {
        long tripLength = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        List<TripDay> tripDays = new ArrayList<>();
        for (int dayNumber = 1; dayNumber <= tripLength; dayNumber++) {
            tripDays.add(new TripDay(trip, dayNumber, startDate.plusDays(dayNumber - 1L)));
        }
        return tripDays;
    }

    private String createInviteCode() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
