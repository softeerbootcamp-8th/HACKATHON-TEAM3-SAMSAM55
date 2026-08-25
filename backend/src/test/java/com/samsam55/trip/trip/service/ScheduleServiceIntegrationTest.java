package com.samsam55.trip.trip.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.samsam55.trip.auth.dto.ActorPrincipal;
import com.samsam55.trip.auth.dto.ParticipantPrincipal;
import com.samsam55.trip.global.support.AbstractMySqlContainerTest;
import com.samsam55.trip.member.entity.User;
import com.samsam55.trip.member.repository.UserRepository;
import com.samsam55.trip.trip.dto.ScheduleResponseDto;
import com.samsam55.trip.trip.dto.VoteResultResponseDto;
import com.samsam55.trip.trip.entity.ItineraryItem;
import com.samsam55.trip.trip.entity.ItineraryItemDecisionType;
import com.samsam55.trip.trip.entity.ItineraryItemStatus;
import com.samsam55.trip.trip.entity.Participant;
import com.samsam55.trip.trip.entity.Trip;
import com.samsam55.trip.trip.entity.TripDay;
import com.samsam55.trip.trip.entity.Vote;
import com.samsam55.trip.trip.entity.VoteOption;
import com.samsam55.trip.trip.repository.ItineraryItemRepository;
import com.samsam55.trip.trip.repository.ParticipantRepository;
import com.samsam55.trip.trip.repository.TripDayRepository;
import com.samsam55.trip.trip.repository.TripRepository;
import com.samsam55.trip.trip.repository.VoteOptionRepository;
import com.samsam55.trip.trip.repository.VoteRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ScheduleServiceIntegrationTest extends AbstractMySqlContainerTest {

    private final ScheduleService scheduleService;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final TripDayRepository tripDayRepository;
    private final ParticipantRepository participantRepository;
    private final ItineraryItemRepository itineraryItemRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final VoteRepository voteRepository;

    @Autowired
    ScheduleServiceIntegrationTest(
            ScheduleService scheduleService,
            UserRepository userRepository,
            TripRepository tripRepository,
            TripDayRepository tripDayRepository,
            ParticipantRepository participantRepository,
            ItineraryItemRepository itineraryItemRepository,
            VoteOptionRepository voteOptionRepository,
            VoteRepository voteRepository
    ) {
        this.scheduleService = scheduleService;
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.tripDayRepository = tripDayRepository;
        this.participantRepository = participantRepository;
        this.itineraryItemRepository = itineraryItemRepository;
        this.voteOptionRepository = voteOptionRepository;
        this.voteRepository = voteRepository;
    }

    @Test
    @DisplayName("참여자 일정과 확정 투표 결과를 날짜·일정·선택지 순서대로 조회한다")
    void 참여자_일정과_확정_투표_결과를_정렬해_조회한다() {
        User host = userRepository.saveAndFlush(new User("schedule-host", "hashed-password"));
        Trip trip = tripRepository.saveAndFlush(new Trip(host, "도쿄 가족여행",
                LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 9, 2, 0, 0),
                3, "schedule-invite-code"));
        TripDay secondDay = tripDayRepository.saveAndFlush(new TripDay(
                trip, 2, LocalDate.of(2026, 9, 2)));
        TripDay firstDay = tripDayRepository.saveAndFlush(new TripDay(
                trip, 1, LocalDate.of(2026, 9, 1)));
        Participant mother = participantRepository.saveAndFlush(new Participant(
                trip, "엄마", LocalDateTime.now()));
        Participant father = participantRepository.saveAndFlush(new Participant(
                trip, "아빠", LocalDateTime.now()));
        Participant child = participantRepository.saveAndFlush(new Participant(
                trip, "첫째", LocalDateTime.now()));

        ItineraryItem pending = itineraryItemRepository.saveAndFlush(new ItineraryItem(
                firstDay, "준비 중 일정", "관광", ItineraryItemDecisionType.VOTE,
                ItineraryItemStatus.PENDING, 1, null));
        ItineraryItem voting = itineraryItemRepository.saveAndFlush(new ItineraryItem(
                firstDay, "점심 식사", "식사", ItineraryItemDecisionType.VOTE,
                ItineraryItemStatus.VOTING, 2, null));
        ItineraryItem confirmed = itineraryItemRepository.saveAndFlush(new ItineraryItem(
                secondDay, "저녁 식사", "식사", ItineraryItemDecisionType.VOTE,
                ItineraryItemStatus.CONFIRMED, 1, null));
        VoteOption votingOption = voteOptionRepository.saveAndFlush(new VoteOption(
                voting, "긴자 스시", "스시 코스", "AI", null, null));
        VoteOption confirmedOption = voteOptionRepository.saveAndFlush(new VoteOption(
                confirmed, "신주쿠 라멘", "진한 돈코츠 라멘", "AI",
                new byte[]{1, 2, 3}, "image/jpeg"));
        VoteOption alternative = voteOptionRepository.saveAndFlush(new VoteOption(
                confirmed, "시부야 우동", "수타 우동", "AI", null, null));
        ReflectionTestUtils.setField(confirmed, "confirmedOption", confirmedOption);
        itineraryItemRepository.flush();

        voteRepository.saveAndFlush(new Vote(votingOption, voting, mother));
        voteRepository.saveAndFlush(new Vote(confirmedOption, confirmed, mother));
        voteRepository.saveAndFlush(new Vote(confirmedOption, confirmed, father));

        ActorPrincipal actor = ActorPrincipal.ofParticipant(
                new ParticipantPrincipal(mother.getId(), trip.getId())
        );
        ScheduleResponseDto schedule = scheduleService.findSchedule(actor, trip.getId());
        VoteResultResponseDto result = scheduleService.findVoteResult(actor, confirmed.getId());

        assertThat(schedule.days()).extracting("dayNumber").containsExactly(1, 2);
        assertThat(schedule.days().getFirst().items()).extracting("id")
                .containsExactly(voting.getId())
                .doesNotContain(pending.getId());
        assertThat(schedule.days().getFirst().items().getFirst().votedCount()).isEqualTo(1);
        assertThat(schedule.days().getLast().items().getFirst().confirmedOption().id())
                .isEqualTo(confirmedOption.getId());
        assertThat(schedule.votingCount()).isEqualTo(1);
        assertThat(result.totalParticipants()).isEqualTo(3);
        assertThat(result.votedCount()).isEqualTo(2);
        assertThat(result.pendingParticipants()).extracting("participantId")
                .containsExactly(child.getId());
        assertThat(result.options()).extracting("optionId")
                .containsExactly(confirmedOption.getId(), alternative.getId());
        assertThat(result.options().getFirst().isConfirmed()).isTrue();
        assertThat(result.options().getFirst().voters()).extracting("participantId")
                .containsExactly(mother.getId(), father.getId());
    }
}
