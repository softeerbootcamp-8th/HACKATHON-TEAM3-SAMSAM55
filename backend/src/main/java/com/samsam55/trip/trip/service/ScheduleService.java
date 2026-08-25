package com.samsam55.trip.trip.service;

import com.samsam55.trip.auth.dto.ActorPrincipal;
import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.trip.dto.ScheduleDayResponseDto;
import com.samsam55.trip.trip.dto.ScheduleItemResponseDto;
import com.samsam55.trip.trip.dto.ScheduleResponseDto;
import com.samsam55.trip.trip.entity.ItineraryItem;
import com.samsam55.trip.trip.entity.ItineraryItemStatus;
import com.samsam55.trip.trip.entity.Trip;
import com.samsam55.trip.trip.entity.Vote;
import com.samsam55.trip.trip.exception.TripErrorType;
import com.samsam55.trip.trip.repository.ItineraryItemRepository;
import com.samsam55.trip.trip.repository.ParticipantRepository;
import com.samsam55.trip.trip.repository.TripDayRepository;
import com.samsam55.trip.trip.repository.TripRepository;
import com.samsam55.trip.trip.repository.VoteRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final TripRepository tripRepository;
    private final TripDayRepository tripDayRepository;
    private final ItineraryItemRepository itineraryItemRepository;
    private final ParticipantRepository participantRepository;
    private final VoteRepository voteRepository;

    /**
     * 인증 주체의 권한에 맞춰 여행의 날짜별 일정 목록을 조회한다.
     *
     * @param actor 현재 인증 주체
     * @param tripId 조회할 여행의 식별자
     * @return 날짜별 일정과 투표 진행 현황
     * @throws ApplicationException 여행이 없거나 조회 권한이 없을 때(TRIP_NOT_FOUND)
     */
    @Transactional(readOnly = true)
    public ScheduleResponseDto findSchedule(ActorPrincipal actor, Long tripId) {
        Trip trip = findAccessibleTrip(actor, tripId);
        boolean isParticipant = actor.actorType() == ActorPrincipal.ActorType.PARTICIPANT;
        List<ItineraryItem> items = itineraryItemRepository
                .findAllByTripIdOrderByDayAndSortOrder(tripId)
                .stream()
                .filter(item -> !isParticipant || item.getStatus() != ItineraryItemStatus.PENDING)
                .toList();
        Map<Long, Integer> votedCounts = countVotersByItem(
                voteRepository.findAllByTripIdWithOptionAndParticipant(tripId)
        );
        long totalParticipants = participantRepository.countByTripId(tripId);
        Map<Long, List<ScheduleItemResponseDto>> itemsByDayId = items.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getTripDay().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(
                                item -> ScheduleItemResponseDto.of(
                                        item,
                                        votedCounts.getOrDefault(item.getId(), 0),
                                        totalParticipants
                                ),
                                Collectors.toList()
                        )
                ));
        List<ScheduleDayResponseDto> days = tripDayRepository
                .findAllByTripIdOrderByDayNumberAsc(tripId)
                .stream()
                .map(day -> ScheduleDayResponseDto.of(
                        day,
                        itemsByDayId.getOrDefault(day.getId(), List.of())
                ))
                .toList();
        int votingCount = (int) items.stream()
                .filter(item -> item.getStatus() == ItineraryItemStatus.VOTING)
                .count();

        return ScheduleResponseDto.of(trip, votingCount, days);
    }

    private Trip findAccessibleTrip(ActorPrincipal actor, Long tripId) {
        if (actor.actorType() == ActorPrincipal.ActorType.HOST) {
            return tripRepository.findByIdAndHostUserId(tripId, actor.userId())
                    .orElseThrow(() -> new ApplicationException(TripErrorType.TRIP_NOT_FOUND));
        }

        if (!tripId.equals(actor.tripId())) {
            throw new ApplicationException(TripErrorType.TRIP_NOT_FOUND);
        }
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ApplicationException(TripErrorType.TRIP_NOT_FOUND));
        participantRepository.findByIdAndTrip(actor.participantId(), trip)
                .orElseThrow(() -> new ApplicationException(TripErrorType.TRIP_NOT_FOUND));
        return trip;
    }

    private Map<Long, Integer> countVotersByItem(List<Vote> votes) {
        return votes.stream()
                .collect(Collectors.groupingBy(
                        vote -> vote.getItineraryItem().getId(),
                        Collectors.collectingAndThen(
                                Collectors.mapping(
                                        vote -> vote.getParticipant().getId(),
                                        Collectors.toSet()
                                ),
                                Set::size
                        )
                ));
    }

}
