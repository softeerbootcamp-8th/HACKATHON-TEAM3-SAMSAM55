package com.samsam55.trip.trip.service;

import com.samsam55.trip.auth.dto.ParticipantPrincipal;
import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.trip.dto.ScheduleDayResponseDto;
import com.samsam55.trip.trip.dto.ScheduleItemResponseDto;
import com.samsam55.trip.trip.dto.ScheduleResponseDto;
import com.samsam55.trip.trip.dto.VoteResultOptionResponseDto;
import com.samsam55.trip.trip.dto.VoteResultResponseDto;
import com.samsam55.trip.trip.entity.ItineraryItem;
import com.samsam55.trip.trip.entity.ItineraryItemStatus;
import com.samsam55.trip.trip.entity.Participant;
import com.samsam55.trip.trip.entity.Trip;
import com.samsam55.trip.trip.entity.Vote;
import com.samsam55.trip.trip.entity.VoteOption;
import com.samsam55.trip.trip.exception.TripErrorType;
import com.samsam55.trip.trip.repository.ItineraryItemRepository;
import com.samsam55.trip.trip.repository.ParticipantRepository;
import com.samsam55.trip.trip.repository.TripDayRepository;
import com.samsam55.trip.trip.repository.TripRepository;
import com.samsam55.trip.trip.repository.VoteOptionRepository;
import com.samsam55.trip.trip.repository.VoteRepository;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
    private final VoteOptionRepository voteOptionRepository;

    /**
     * 현재 참여자가 속한 여행의 날짜별 일정 목록을 조회한다.
     *
     * @param participant 현재 참여자
     * @param tripId 조회할 여행의 식별자
     * @return 날짜별 일정과 투표 진행 현황
     * @throws ApplicationException 여행이 없거나 조회 권한이 없을 때(TRIP_NOT_FOUND)
     */
    @Transactional(readOnly = true)
    public ScheduleResponseDto findSchedule(ParticipantPrincipal participant, Long tripId) {
        Trip trip = findAccessibleTrip(participant, tripId);
        List<ItineraryItem> items = itineraryItemRepository
                .findAllByTripIdOrderByDayAndSortOrder(tripId)
                .stream()
                .filter(item -> item.getStatus() != ItineraryItemStatus.PENDING)
                .toList();
        List<Participant> participants = participantRepository.findAllByTripOrderById(trip);
        Map<Long, Integer> votedCounts = voteRepository.countDistinctParticipantsByTripId(tripId)
                .stream()
                .collect(Collectors.toMap(
                        VoteRepository.ItineraryItemVoteCount::getItemId,
                        voteCount -> Math.toIntExact(voteCount.getVotedCount())
                ));
        long totalParticipants = participants.size();
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

    /**
     * 일정 항목의 참여자별 투표 현황과 선택지별 결과를 조회한다.
     *
     * @param participant 현재 참여자
     * @param itemId 조회할 일정 항목의 식별자
     * @return 일정 정보와 투표 결과
     * @throws ApplicationException 일정이 없거나 조회 권한이 없을 때(ITINERARY_ITEM_NOT_FOUND)
     */
    @Transactional(readOnly = true)
    public VoteResultResponseDto findVoteResult(ParticipantPrincipal participant, Long itemId) {
        ItineraryItem item = itineraryItemRepository.findByIdWithTripAndConfirmedOption(itemId)
                .orElseThrow(() -> new ApplicationException(TripErrorType.ITINERARY_ITEM_NOT_FOUND));
        validateItemAccess(participant, item);

        Trip trip = item.getTripDay().getTrip();
        List<Participant> participants = participantRepository.findAllByTripOrderById(trip);
        Map<Long, Participant> participantsById = participants.stream()
                .collect(Collectors.toMap(
                        Participant::getId,
                        tripParticipant -> tripParticipant,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        List<VoteOption> options = voteOptionRepository.findAllByItineraryItemIdOrderByIdAsc(itemId);
        Set<Long> optionIds = options.stream()
                .map(VoteOption::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Vote> votesByParticipantId = new LinkedHashMap<>();
        voteRepository.findAllByItineraryItemIdWithOptionAndParticipant(itemId).stream()
                .filter(vote -> participantsById.containsKey(vote.getParticipant().getId()))
                .filter(vote -> optionIds.contains(vote.getOption().getId()))
                .forEach(vote -> votesByParticipantId.putIfAbsent(vote.getParticipant().getId(), vote));

        List<Participant> pendingParticipants = participants.stream()
                .filter(tripParticipant -> !votesByParticipantId.containsKey(tripParticipant.getId()))
                .toList();
        Long confirmedOptionId = item.getConfirmedOption() == null
                ? null
                : item.getConfirmedOption().getId();
        List<VoteResultOptionResponseDto> optionResults = options.stream()
                .map(option -> VoteResultOptionResponseDto.of(
                        option,
                        findVoters(option, participants, votesByParticipantId),
                        option.getId().equals(confirmedOptionId)
                ))
                .toList();

        return VoteResultResponseDto.of(item, participants, pendingParticipants, optionResults);
    }

    private Trip findAccessibleTrip(ParticipantPrincipal participant, Long tripId) {
        if (!tripId.equals(participant.tripId())) {
            throw new ApplicationException(TripErrorType.TRIP_NOT_FOUND);
        }
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ApplicationException(TripErrorType.TRIP_NOT_FOUND));
        participantRepository.findByIdAndTrip(participant.participantId(), trip)
                .orElseThrow(() -> new ApplicationException(TripErrorType.TRIP_NOT_FOUND));
        return trip;
    }

    private void validateItemAccess(ParticipantPrincipal participant, ItineraryItem item) {
        Trip trip = item.getTripDay().getTrip();
        if (item.getStatus() == ItineraryItemStatus.PENDING
                || !trip.getId().equals(participant.tripId())) {
            throw new ApplicationException(TripErrorType.ITINERARY_ITEM_NOT_FOUND);
        }
        participantRepository.findByIdAndTrip(participant.participantId(), trip)
                .orElseThrow(() -> new ApplicationException(TripErrorType.ITINERARY_ITEM_NOT_FOUND));
    }

    private List<Participant> findVoters(
            VoteOption option,
            List<Participant> participants,
            Map<Long, Vote> votesByParticipantId
    ) {
        return participants.stream()
                .filter(participant -> {
                    Vote vote = votesByParticipantId.get(participant.getId());
                    return vote != null && vote.getOption().getId().equals(option.getId());
                })
                .toList();
    }

}
