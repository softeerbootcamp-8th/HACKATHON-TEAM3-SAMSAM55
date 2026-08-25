package com.samsam55.trip.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.samsam55.trip.auth.dto.ParticipantPrincipal;
import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.member.entity.User;
import com.samsam55.trip.trip.dto.ScheduleResponseDto;
import com.samsam55.trip.trip.dto.VoteResultOptionResponseDto;
import com.samsam55.trip.trip.dto.VoteResultParticipantResponseDto;
import com.samsam55.trip.trip.dto.VoteResultResponseDto;
import com.samsam55.trip.trip.entity.ItineraryItem;
import com.samsam55.trip.trip.entity.ItineraryItemDecisionType;
import com.samsam55.trip.trip.entity.ItineraryItemStatus;
import com.samsam55.trip.trip.entity.Participant;
import com.samsam55.trip.trip.entity.Trip;
import com.samsam55.trip.trip.entity.TripDay;
import com.samsam55.trip.trip.entity.Vote;
import com.samsam55.trip.trip.entity.VoteOption;
import com.samsam55.trip.trip.exception.TripErrorType;
import com.samsam55.trip.trip.repository.ItineraryItemRepository;
import com.samsam55.trip.trip.repository.ParticipantRepository;
import com.samsam55.trip.trip.repository.TripDayRepository;
import com.samsam55.trip.trip.repository.TripRepository;
import com.samsam55.trip.trip.repository.VoteOptionRepository;
import com.samsam55.trip.trip.repository.VoteRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripDayRepository tripDayRepository;

    @Mock
    private ItineraryItemRepository itineraryItemRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private VoteOptionRepository voteOptionRepository;

    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        scheduleService = new ScheduleService(
                tripRepository,
                tripDayRepository,
                itineraryItemRepository,
                participantRepository,
                voteRepository,
                voteOptionRepository
        );
    }

    @Test
    @DisplayName("PARTICIPANT는 PENDING 일정을 제외하고 VOTING 일정 수만 반환한다")
    void PARTICIPANT는_PENDING_일정을_제외하고_VOTING_일정_수만_반환한다() {
        ScheduleFixture fixture = fixture();
        List<ItineraryItem> items = List.of(
                fixture.pendingItem,
                fixture.votingItem,
                fixture.votedItem,
                fixture.confirmedItem
        );
        stubParticipantSchedule(fixture, 101L, items, List.of());

        ScheduleResponseDto response = scheduleService.findSchedule(participantActor(101L, 1L), 1L);

        assertThat(response.votingCount()).isEqualTo(1);
        assertThat(response.days().getFirst().items())
                .extracting("status")
                .containsExactly("VOTING", "VOTED", "CONFIRMED");
        assertThat(response.days().getFirst().items())
                .extracting("name")
                .doesNotContain("준비 중 일정");
    }

    @Test
    @DisplayName("일정 목록은 DB에서 집계한 일정별 투표 참여자 수를 반환한다")
    void 일정_목록은_DB에서_집계한_일정별_투표_참여자_수를_반환한다() {
        ScheduleFixture fixture = fixture();
        List<VoteRepository.ItineraryItemVoteCount> voteCounts = List.of(
                voteCount(fixture.votingItem.getId(), 2)
        );
        stubParticipantSchedule(fixture, 101L, List.of(fixture.votingItem), voteCounts);

        ScheduleResponseDto response = scheduleService.findSchedule(participantActor(101L, 1L), 1L);

        assertThat(response.days().getFirst().items().getFirst().votedCount()).isEqualTo(2);
        assertThat(response.days().getFirst().items().getFirst().totalParticipants()).isEqualTo(3);
        verify(voteRepository).countDistinctParticipantsByTripId(1L);
    }

    @Test
    @DisplayName("다른 여행의 PARTICIPANT는 일정 목록을 조회할 수 없다")
    void 다른_여행의_PARTICIPANT는_일정_목록을_조회할_수_없다() {
        ScheduleFixture fixture = fixture();

        assertThatThrownBy(() -> scheduleService.findSchedule(participantActor(101L, 2L), 1L))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.TRIP_NOT_FOUND));

        verify(tripRepository, never()).findById(1L);
        verifyNoInteractions(tripDayRepository, itineraryItemRepository, participantRepository, voteRepository);
    }

    @Test
    @DisplayName("유효하지 않은 PARTICIPANT는 일정 목록을 조회할 수 없다")
    void 유효하지_않은_PARTICIPANT는_일정_목록을_조회할_수_없다() {
        ScheduleFixture fixture = fixture();
        when(tripRepository.findById(1L)).thenReturn(Optional.of(fixture.trip));
        when(participantRepository.findByIdAndTrip(999L, fixture.trip)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.findSchedule(participantActor(999L, 1L), 1L))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.TRIP_NOT_FOUND));

        verifyNoInteractions(tripDayRepository, itineraryItemRepository, voteRepository);
    }

    @Test
    @DisplayName("진행 중 일정 상세는 참여자·미투표자와 선택지별 투표자를 반환하고 0표 선택지도 포함한다")
    void 진행_중_일정_상세는_참여자_미투표자와_선택지별_투표자를_반환하고_0표_선택지도_포함한다() {
        ScheduleFixture fixture = fixture();
        List<Vote> votes = List.of(
                vote(401L, fixture.votingOptionA, fixture.votingItem, fixture.firstParticipant),
                vote(402L, fixture.votingOptionB, fixture.votingItem, fixture.firstParticipant),
                vote(403L, fixture.votingOptionB, fixture.votingItem, fixture.secondParticipant),
                vote(404L, fixture.votingOptionA, fixture.votingItem, fixture.outsiderParticipant)
        );
        stubParticipantVoteResult(fixture, 101L, fixture.votingItem, fixture.votingOptions, votes);

        VoteResultResponseDto response = scheduleService.findVoteResult(
                participantActor(101L, 1L), fixture.votingItem.getId());

        assertThat(response.itemId()).isEqualTo(fixture.votingItem.getId());
        assertThat(response.name()).isEqualTo("진행 중 일정");
        assertThat(response.category()).isEqualTo("식사");
        assertThat(response.status()).isEqualTo("VOTING");
        assertThat(response.dayNumber()).isEqualTo(1);
        assertThat(response.totalParticipants()).isEqualTo(3);
        assertThat(response.votedCount()).isEqualTo(2);
        assertThat(response.optionCount()).isEqualTo(3);
        assertThat(response.confirmedOptionId()).isNull();
        assertThat(response.participants())
                .extracting(VoteResultParticipantResponseDto::participantId)
                .containsExactly(101L, 102L, 103L);
        assertThat(response.pendingParticipants())
                .extracting(VoteResultParticipantResponseDto::participantId)
                .containsExactly(103L);

        VoteResultOptionResponseDto optionA = response.options().get(0);
        VoteResultOptionResponseDto optionB = response.options().get(1);
        VoteResultOptionResponseDto optionZero = response.options().get(2);
        assertThat(optionA.voteCount()).isEqualTo(1);
        assertThat(optionA.voters()).extracting(VoteResultParticipantResponseDto::participantId)
                .containsExactly(101L);
        assertThat(optionB.voteCount()).isEqualTo(1);
        assertThat(optionB.voters()).extracting(VoteResultParticipantResponseDto::participantId)
                .containsExactly(102L);
        assertThat(optionZero.voteCount()).isZero();
        assertThat(optionZero.voters()).isEmpty();
        assertThat(response.options()).allSatisfy(option -> assertThat(option.isConfirmed()).isFalse());
    }

    @Test
    @DisplayName("확정 일정 상세는 confirmedOption과 확정 선택지별 투표자를 반환한다")
    void 확정_일정_상세는_confirmedOption과_확정_선택지별_투표자를_반환한다() {
        ScheduleFixture fixture = fixture();
        List<VoteOption> options = List.of(fixture.confirmedOption, fixture.confirmedAlternative);
        List<Vote> votes = List.of(
                vote(501L, fixture.confirmedOption, fixture.confirmedItem, fixture.firstParticipant),
                vote(502L, fixture.confirmedOption, fixture.confirmedItem, fixture.secondParticipant)
        );
        stubParticipantVoteResult(fixture, 101L, fixture.confirmedItem, options, votes);

        VoteResultResponseDto response = scheduleService.findVoteResult(
                participantActor(101L, 1L), fixture.confirmedItem.getId());

        assertThat(response.status()).isEqualTo("CONFIRMED");
        assertThat(response.confirmedOptionId()).isEqualTo(fixture.confirmedOption.getId());
        assertThat(response.totalParticipants()).isEqualTo(3);
        assertThat(response.votedCount()).isEqualTo(2);
        assertThat(response.pendingParticipants())
                .extracting(VoteResultParticipantResponseDto::participantId)
                .containsExactly(103L);
        assertThat(response.options()).hasSize(2);
        assertThat(response.options().get(0).isConfirmed()).isTrue();
        assertThat(response.options().get(0).voteCount()).isEqualTo(2);
        assertThat(response.options().get(0).voters())
                .extracting(VoteResultParticipantResponseDto::participantId)
                .containsExactly(101L, 102L);
        assertThat(response.options().get(1).isConfirmed()).isFalse();
        assertThat(response.options().get(1).voteCount()).isZero();
    }

    @Test
    @DisplayName("PARTICIPANT는 PENDING 일정 상세를 조회할 수 없다")
    void PARTICIPANT는_PENDING_일정_상세를_조회할_수_없다() {
        ScheduleFixture fixture = fixture();
        when(itineraryItemRepository.findByIdWithTripAndConfirmedOption(fixture.pendingItem.getId()))
                .thenReturn(Optional.of(fixture.pendingItem));

        assertThatThrownBy(() -> scheduleService.findVoteResult(
                participantActor(101L, 1L), fixture.pendingItem.getId()))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.ITINERARY_ITEM_NOT_FOUND));

        verifyNoInteractions(participantRepository, voteRepository, voteOptionRepository);
    }

    @Test
    @DisplayName("유효하지 않은 PARTICIPANT는 일정 상세를 조회할 수 없다")
    void 유효하지_않은_PARTICIPANT는_일정_상세를_조회할_수_없다() {
        ScheduleFixture fixture = fixture();
        when(itineraryItemRepository.findByIdWithTripAndConfirmedOption(fixture.votingItem.getId()))
                .thenReturn(Optional.of(fixture.votingItem));
        when(participantRepository.findByIdAndTrip(999L, fixture.trip)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.findVoteResult(
                participantActor(999L, 1L), fixture.votingItem.getId()))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.ITINERARY_ITEM_NOT_FOUND));

        verifyNoInteractions(voteRepository, voteOptionRepository);
    }

    private void stubParticipantSchedule(
            ScheduleFixture fixture,
            Long participantId,
            List<ItineraryItem> items,
            List<VoteRepository.ItineraryItemVoteCount> voteCounts
    ) {
        when(tripRepository.findById(1L)).thenReturn(Optional.of(fixture.trip));
        when(participantRepository.findByIdAndTrip(participantId, fixture.trip))
                .thenReturn(Optional.of(participantById(fixture, participantId)));
        stubScheduleData(fixture, items, voteCounts);
    }

    private void stubScheduleData(
            ScheduleFixture fixture,
            List<ItineraryItem> items,
            List<VoteRepository.ItineraryItemVoteCount> voteCounts
    ) {
        when(itineraryItemRepository.findAllByTripIdOrderByDayAndSortOrder(1L)).thenReturn(items);
        when(participantRepository.findAllByTripOrderById(fixture.trip)).thenReturn(fixture.participants);
        when(voteRepository.countDistinctParticipantsByTripId(1L)).thenReturn(voteCounts);
        when(tripDayRepository.findAllByTripIdOrderByDayNumberAsc(1L)).thenReturn(List.of(fixture.day));
    }

    private VoteRepository.ItineraryItemVoteCount voteCount(Long itemId, long votedCount) {
        return new VoteRepository.ItineraryItemVoteCount() {
            @Override
            public Long getItemId() {
                return itemId;
            }

            @Override
            public long getVotedCount() {
                return votedCount;
            }
        };
    }

    private void stubParticipantVoteResult(
            ScheduleFixture fixture,
            Long participantId,
            ItineraryItem item,
            List<VoteOption> options,
            List<Vote> votes
    ) {
        when(itineraryItemRepository.findByIdWithTripAndConfirmedOption(item.getId()))
                .thenReturn(Optional.of(item));
        when(participantRepository.findByIdAndTrip(participantId, fixture.trip))
                .thenReturn(Optional.of(participantById(fixture, participantId)));
        when(participantRepository.findAllByTripOrderById(fixture.trip)).thenReturn(fixture.participants);
        when(voteOptionRepository.findAllByItineraryItemIdOrderByIdAsc(item.getId())).thenReturn(options);
        when(voteRepository.findAllByItineraryItemIdWithOptionAndParticipant(item.getId())).thenReturn(votes);
    }

    private ParticipantPrincipal participantActor(Long participantId, Long tripId) {
        return new ParticipantPrincipal(participantId, tripId);
    }

    private Participant participantById(ScheduleFixture fixture, Long participantId) {
        return fixture.participants.stream()
                .filter(participant -> participant.getId().equals(participantId))
                .findFirst()
                .orElseThrow();
    }

    private ScheduleFixture fixture() {
        User host = user(10L);
        Trip trip = trip(1L, host);
        TripDay day = day(11L, trip, 1, LocalDate.of(2026, 9, 1));
        Participant first = participant(101L, trip, "엄마");
        Participant second = participant(102L, trip, "아빠");
        Participant third = participant(103L, trip, "할머니");
        Participant outsider = participant(999L, trip(2L, user(20L)), "다른 여행 참여자");

        ItineraryItem pending = item(201L, day, "준비 중 일정", ItineraryItemStatus.PENDING, 1, null);
        ItineraryItem voting = item(202L, day, "진행 중 일정", ItineraryItemStatus.VOTING, 2, null);
        ItineraryItem voted = item(203L, day, "투표 완료 일정", ItineraryItemStatus.VOTED, 3, null);
        ItineraryItem confirmed = item(204L, day, "확정 일정", ItineraryItemStatus.CONFIRMED, 4, null);

        VoteOption votingOptionA = option(301L, voting, "스시", "스시 설명", null);
        VoteOption votingOptionB = option(302L, voting, "라멘", "라멘 설명", null);
        VoteOption votingOptionZero = option(303L, voting, "우동", "우동 설명", null);
        VoteOption confirmedOption = option(
                304L,
                confirmed,
                "스시 오마카세 긴자점",
                "신선한 제철 재료로 만든 프리미엄 스시 코스",
                "image".getBytes(StandardCharsets.UTF_8)
        );
        VoteOption confirmedAlternative = option(305L, confirmed, "라멘 이치란 신주쿠점", "라멘 설명", null);
        ReflectionTestUtils.setField(confirmed, "confirmedOption", confirmedOption);

        return new ScheduleFixture(
                trip,
                day,
                List.of(first, second, third),
                first,
                second,
                third,
                outsider,
                pending,
                voting,
                voted,
                confirmed,
                votingOptionA,
                votingOptionB,
                votingOptionZero,
                confirmedOption,
                confirmedAlternative,
                List.of(votingOptionA, votingOptionB, votingOptionZero)
        );
    }

    private User user(Long id) {
        User user = new User("user-" + id, "hashed-password");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Trip trip(Long id, User host) {
        Trip trip = new Trip(
                host,
                "도쿄 가족여행",
                LocalDateTime.of(2026, 9, 1, 0, 0),
                LocalDateTime.of(2026, 9, 3, 0, 0),
                3,
                "invite-code-" + id
        );
        ReflectionTestUtils.setField(trip, "id", id);
        return trip;
    }

    private TripDay day(Long id, Trip trip, int dayNumber, LocalDate date) {
        TripDay day = new TripDay(trip, dayNumber, date);
        ReflectionTestUtils.setField(day, "id", id);
        return day;
    }

    private Participant participant(Long id, Trip trip, String roleName) {
        Participant participant = new Participant(trip, roleName, LocalDateTime.now());
        ReflectionTestUtils.setField(participant, "id", id);
        return participant;
    }

    private ItineraryItem item(
            Long id,
            TripDay day,
            String name,
            ItineraryItemStatus status,
            int sortOrder,
            VoteOption confirmedOption
    ) {
        ItineraryItem item = new ItineraryItem(
                day,
                name,
                "식사",
                ItineraryItemDecisionType.VOTE,
                status,
                sortOrder,
                confirmedOption
        );
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    private VoteOption option(
            Long id,
            ItineraryItem item,
            String name,
            String description,
            byte[] image
    ) {
        VoteOption option = new VoteOption(item, name, description, "AI", image, image == null ? null : "image/jpeg");
        ReflectionTestUtils.setField(option, "id", id);
        return option;
    }

    private Vote vote(Long id, VoteOption option, ItineraryItem item, Participant participant) {
        Vote vote = new Vote(option, item, participant);
        ReflectionTestUtils.setField(vote, "id", id);
        return vote;
    }

    private record ScheduleFixture(
            Trip trip,
            TripDay day,
            List<Participant> participants,
            Participant firstParticipant,
            Participant secondParticipant,
            Participant thirdParticipant,
            Participant outsiderParticipant,
            ItineraryItem pendingItem,
            ItineraryItem votingItem,
            ItineraryItem votedItem,
            ItineraryItem confirmedItem,
            VoteOption votingOptionA,
            VoteOption votingOptionB,
            VoteOption votingOptionZero,
            VoteOption confirmedOption,
            VoteOption confirmedAlternative,
            List<VoteOption> votingOptions
    ) {
    }
}
