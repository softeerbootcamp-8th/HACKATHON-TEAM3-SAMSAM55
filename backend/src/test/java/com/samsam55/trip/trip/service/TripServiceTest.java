package com.samsam55.trip.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.member.repository.UserRepository;
import com.samsam55.trip.trip.dto.TripCreateRequestDto;
import com.samsam55.trip.trip.dto.TripDetailResponseDto;
import com.samsam55.trip.trip.dto.TripListResponseDto;
import com.samsam55.trip.trip.entity.ItineraryItem;
import com.samsam55.trip.trip.entity.ItineraryItemDecisionType;
import com.samsam55.trip.trip.entity.ItineraryItemStatus;
import com.samsam55.trip.trip.entity.Trip;
import com.samsam55.trip.trip.entity.TripDay;
import com.samsam55.trip.trip.repository.ParticipantRepository;
import com.samsam55.trip.trip.repository.ItineraryItemRepository;
import com.samsam55.trip.trip.repository.TripDayRepository;
import com.samsam55.trip.trip.repository.TripRepository;
import com.samsam55.trip.trip.repository.VoteOptionRepository;
import com.samsam55.trip.trip.repository.VoteRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripDayRepository tripDayRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItineraryItemRepository itineraryItemRepository;

    @Mock
    private VoteOptionRepository voteOptionRepository;

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private Trip trip;

    @Mock
    private TripDay tripDay;

    @Mock
    private ItineraryItem itineraryItem;

    @Test
    @DisplayName("로그인한 사용자가 방장인 여행만 목록으로 반환한다")
    void 로그인한_사용자가_방장인_여행만_목록으로_반환한다() {
        when(tripRepository.findAllByHostUserIdOrderByStartDateAscIdAsc(1L)).thenReturn(List.of(trip));
        when(trip.getId()).thenReturn(1L);
        when(trip.getTitle()).thenReturn("제주 가족 여행");
        when(trip.getStartDate()).thenReturn(LocalDateTime.of(2026, 9, 1, 0, 0));
        when(trip.getEndDate()).thenReturn(LocalDateTime.of(2026, 9, 3, 0, 0));
        when(trip.getCompanionCount()).thenReturn(2);
        when(itineraryItemRepository.countByTripDayTripId(1L)).thenReturn(8L);
        when(itineraryItemRepository.countByTripDayTripIdAndStatus(1L, ItineraryItemStatus.CONFIRMED))
                .thenReturn(5L);

        TripListResponseDto response = service().findTrips(1L);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().id()).isEqualTo(1L);
        assertThat(response.items().getFirst().title()).isEqualTo("제주 가족 여행");
        assertThat(response.items().getFirst().startDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(response.items().getFirst().endDate()).isEqualTo(LocalDate.of(2026, 9, 3));
        assertThat(response.items().getFirst().companionCount()).isEqualTo(2);
        assertThat(response.items().getFirst().totalItems()).isEqualTo(8L);
        assertThat(response.items().getFirst().confirmedItems()).isEqualTo(5L);
        assertThat(response.items().getFirst().progressPercent()).isEqualTo(62);
        verify(tripRepository).findAllByHostUserIdOrderByStartDateAscIdAsc(1L);
        verify(itineraryItemRepository).countByTripDayTripId(1L);
        verify(itineraryItemRepository)
                .countByTripDayTripIdAndStatus(1L, ItineraryItemStatus.CONFIRMED);
    }

    @Test
    @DisplayName("일정이 없는 여행의 진척률은 0으로 반환한다")
    void 일정이_없는_여행의_진척률은_0으로_반환한다() {
        when(tripRepository.findAllByHostUserIdOrderByStartDateAscIdAsc(1L)).thenReturn(List.of(trip));
        when(trip.getId()).thenReturn(1L);
        when(trip.getTitle()).thenReturn("일정 없는 여행");
        when(trip.getStartDate()).thenReturn(LocalDateTime.of(2026, 9, 1, 0, 0));
        when(trip.getEndDate()).thenReturn(LocalDateTime.of(2026, 9, 1, 0, 0));
        when(trip.getCompanionCount()).thenReturn(1);
        when(itineraryItemRepository.countByTripDayTripId(1L)).thenReturn(0L);
        when(itineraryItemRepository.countByTripDayTripIdAndStatus(1L, ItineraryItemStatus.CONFIRMED))
                .thenReturn(0L);

        TripListResponseDto response = service().findTrips(1L);

        assertThat(response.items().getFirst().totalItems()).isZero();
        assertThat(response.items().getFirst().confirmedItems()).isZero();
        assertThat(response.items().getFirst().progressPercent()).isZero();
    }

    @Test
    @DisplayName("방장에게 여행이 없으면 빈 items를 반환한다")
    void 방장에게_여행이_없으면_빈_items를_반환한다() {
        when(tripRepository.findAllByHostUserIdOrderByStartDateAscIdAsc(1L)).thenReturn(List.of());

        TripListResponseDto response = service().findTrips(1L);

        assertThat(response.items()).isEmpty();
    }

    @Test
    @DisplayName("진행 중인 여행 중 가장 임박한 여행을 먼저 반환한다")
    void 진행_중인_여행_중_가장_임박한_여행을_먼저_반환한다() {
        LocalDate today = LocalDate.now();
        Trip endedTrip = org.mockito.Mockito.mock(Trip.class);
        Trip nearestTrip = org.mockito.Mockito.mock(Trip.class);
        Trip laterTrip = org.mockito.Mockito.mock(Trip.class);
        when(tripRepository.findAllByHostUserIdOrderByStartDateAscIdAsc(1L))
                .thenReturn(List.of(endedTrip, nearestTrip, laterTrip));
        when(endedTrip.getId()).thenReturn(1L);
        when(endedTrip.getTitle()).thenReturn("지난 여행");
        when(endedTrip.getStartDate()).thenReturn(today.minusDays(10).atStartOfDay());
        when(endedTrip.getEndDate()).thenReturn(today.minusDays(7).atStartOfDay());
        when(endedTrip.getCompanionCount()).thenReturn(2);
        when(nearestTrip.getId()).thenReturn(2L);
        when(nearestTrip.getTitle()).thenReturn("가장 임박한 여행");
        when(nearestTrip.getStartDate()).thenReturn(today.plusDays(18).atStartOfDay());
        when(nearestTrip.getEndDate()).thenReturn(today.plusDays(21).atStartOfDay());
        when(nearestTrip.getCompanionCount()).thenReturn(4);
        when(laterTrip.getId()).thenReturn(3L);
        when(laterTrip.getTitle()).thenReturn("다른 진행 중 여행");
        when(laterTrip.getStartDate()).thenReturn(today.plusDays(30).atStartOfDay());
        when(laterTrip.getEndDate()).thenReturn(today.plusDays(33).atStartOfDay());
        when(laterTrip.getCompanionCount()).thenReturn(3);

        TripListResponseDto response = service().findTrips(1L);

        assertThat(response.items()).extracting("title")
                .containsExactly("가장 임박한 여행", "다른 진행 중 여행", "지난 여행");
        assertThat(response.items()).extracting("startDate")
                .containsExactly(today.plusDays(18), today.plusDays(30), today.minusDays(10));
    }

    @Test
    @DisplayName("시작일이 종료일보다 늦으면 여행을 생성하지 않는다")
    void 시작일이_종료일보다_늦으면_여행을_생성하지_않는다() {
        TripCreateRequestDto request = new TripCreateRequestDto(
                "잘못된 여행",
                LocalDate.of(2026, 9, 3),
                LocalDate.of(2026, 9, 1),
                List.of("엄마")
        );

        assertThatThrownBy(() -> service().createTrip(1L, request))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType().getCode()).isEqualTo("INVALID_TRIP_PERIOD"));
        verifyNoInteractions(
                userRepository,
                tripRepository,
                tripDayRepository,
                participantRepository,
                itineraryItemRepository,
                voteOptionRepository,
                voteRepository
        );
    }

    @Test
    @DisplayName("시작일이 오늘보다 이전이면 여행을 생성하지 않는다")
    void 시작일이_오늘보다_이전이면_여행을_생성하지_않는다() {
        LocalDate today = LocalDate.now();
        TripCreateRequestDto request = new TripCreateRequestDto(
                "지난 여행",
                today.minusDays(1),
                today,
                List.of("엄마")
        );

        assertThatThrownBy(() -> service().createTrip(1L, request))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType().getCode()).isEqualTo("INVALID_TRIP_PERIOD"));
        verifyNoInteractions(
                userRepository,
                tripRepository,
                tripDayRepository,
                participantRepository,
                itineraryItemRepository,
                voteOptionRepository,
                voteRepository
        );
    }

    @Test
    @DisplayName("여행 기간이 최대 365일을 초과하면 여행을 생성하지 않는다")
    void 여행_기간이_최대_365일을_초과하면_여행을_생성하지_않는다() {
        LocalDate startDate = LocalDate.now().plusDays(1);
        TripCreateRequestDto request = new TripCreateRequestDto(
                "너무 긴 여행",
                startDate,
                startDate.plusDays(365),
                List.of("엄마")
        );

        assertThatThrownBy(() -> service().createTrip(1L, request))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType().getCode()).isEqualTo("INVALID_TRIP_PERIOD"));
        verifyNoInteractions(
                userRepository,
                tripRepository,
                tripDayRepository,
                participantRepository,
                itineraryItemRepository,
                voteOptionRepository,
                voteRepository
        );
    }

    @Test
    @DisplayName("방장이 여행을 삭제하면 하위 데이터를 정해진 순서로 삭제한다")
    void 방장이_여행을_삭제하면_하위_데이터를_정해진_순서로_삭제한다() {
        when(tripRepository.findByIdAndHostUserId(1L, 1L)).thenReturn(java.util.Optional.of(trip));

        service().deleteTrip(1L, 1L);

        var inOrder = org.mockito.Mockito.inOrder(
                voteRepository,
                itineraryItemRepository,
                voteOptionRepository,
                participantRepository,
                tripDayRepository,
                tripRepository
        );
        inOrder.verify(voteRepository).deleteAllByTripId(1L);
        inOrder.verify(itineraryItemRepository).clearConfirmedOptionByTripId(1L);
        inOrder.verify(voteOptionRepository).deleteAllByTripId(1L);
        inOrder.verify(itineraryItemRepository).deleteAllByTripId(1L);
        inOrder.verify(participantRepository).deleteAllByTripId(1L);
        inOrder.verify(tripDayRepository).deleteAllByTripId(1L);
        inOrder.verify(tripRepository).delete(trip);
        inOrder.verify(tripRepository).flush();
    }

    @Test
    @DisplayName("다른 사용자의 여행은 존재하지 않는 것처럼 삭제할 수 없다")
    void 다른_사용자의_여행은_존재하지_않는_것처럼_삭제할_수_없다() {
        when(tripRepository.findByIdAndHostUserId(1L, 2L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service().deleteTrip(2L, 1L))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType().getCode()).isEqualTo("TRIP_NOT_FOUND"));
        verifyNoInteractions(
                itineraryItemRepository,
                voteOptionRepository,
                voteRepository,
                participantRepository,
                tripDayRepository
        );
    }

    @Test
    @DisplayName("방장인 사용자는 여행 상세 정보를 조회할 수 있다")
    void 방장인_사용자는_여행_상세_정보를_조회할_수_있다() {
        when(tripRepository.findByIdAndHostUserId(1L, 1L)).thenReturn(java.util.Optional.of(trip));
        when(trip.getId()).thenReturn(1L);
        when(trip.getTitle()).thenReturn("제주 가족 여행");
        when(trip.getStartDate()).thenReturn(LocalDateTime.of(2026, 9, 1, 0, 0));
        when(trip.getEndDate()).thenReturn(LocalDateTime.of(2026, 9, 3, 0, 0));
        when(trip.getCompanionCount()).thenReturn(2);
        when(trip.getInviteCode()).thenReturn("invite-code");
        when(tripDayRepository.findAllByTripIdOrderByDayNumberAsc(1L)).thenReturn(List.of());
        when(itineraryItemRepository.findAllByTripIdOrderByDayAndSortOrder(1L)).thenReturn(List.of());

        TripDetailResponseDto response = service().findTrip(1L, 1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("제주 가족 여행");
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 9, 3));
        assertThat(response.companionCount()).isEqualTo(2);
        assertThat(response.inviteCode()).isEqualTo("invite-code");
        assertThat(response.days()).isEmpty();
    }

    @Test
    @DisplayName("방장이 아닌 사용자는 여행 상세 정보를 조회할 수 없다")
    void 방장이_아닌_사용자는_여행_상세_정보를_조회할_수_없다() {
        when(tripRepository.findByIdAndHostUserId(1L, 2L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service().findTrip(2L, 1L))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType().getCode()).isEqualTo("TRIP_NOT_FOUND"));
    }

    @Test
    @DisplayName("여행 상세 조회 결과에 날짜별 일정 항목을 매핑한다")
    void 여행_상세_조회_결과에_날짜별_일정_항목을_매핑한다() {
        when(tripRepository.findByIdAndHostUserId(1L, 1L)).thenReturn(java.util.Optional.of(trip));
        when(trip.getId()).thenReturn(1L);
        when(trip.getTitle()).thenReturn("도쿄 가족여행");
        when(trip.getStartDate()).thenReturn(LocalDateTime.of(2026, 9, 1, 0, 0));
        when(trip.getEndDate()).thenReturn(LocalDateTime.of(2026, 9, 3, 0, 0));
        when(trip.getCompanionCount()).thenReturn(2);
        when(trip.getInviteCode()).thenReturn("invite-code");
        when(tripDayRepository.findAllByTripIdOrderByDayNumberAsc(1L)).thenReturn(List.of(tripDay));
        when(itineraryItemRepository.findAllByTripIdOrderByDayAndSortOrder(1L))
                .thenReturn(List.of(itineraryItem));
        when(tripDay.getId()).thenReturn(10L);
        when(tripDay.getDayNumber()).thenReturn(1);
        when(tripDay.getTripDate()).thenReturn(LocalDate.of(2026, 9, 1));
        when(itineraryItem.getTripDay()).thenReturn(tripDay);
        when(itineraryItem.getId()).thenReturn(100L);
        when(itineraryItem.getName()).thenReturn("점심 식사");
        when(itineraryItem.getCategory()).thenReturn("식사");
        when(itineraryItem.getStatus()).thenReturn(ItineraryItemStatus.VOTING);
        when(itineraryItem.getDecisionType()).thenReturn(ItineraryItemDecisionType.VOTE);

        TripDetailResponseDto response = service().findTrip(1L, 1L);

        assertThat(response.days()).hasSize(1);
        assertThat(response.days().getFirst().id()).isEqualTo(10L);
        assertThat(response.days().getFirst().dayNumber()).isEqualTo(1);
        assertThat(response.days().getFirst().date()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(response.days().getFirst().items()).hasSize(1);
        assertThat(response.days().getFirst().items().getFirst().id()).isEqualTo(100L);
        assertThat(response.days().getFirst().items().getFirst().name()).isEqualTo("점심 식사");
        assertThat(response.days().getFirst().items().getFirst().category()).isEqualTo("식사");
        assertThat(response.days().getFirst().items().getFirst().status()).isEqualTo("VOTING");
        assertThat(response.days().getFirst().items().getFirst().decisionType()).isEqualTo("VOTE");
        verify(tripDayRepository).findAllByTripIdOrderByDayNumberAsc(1L);
        verify(itineraryItemRepository).findAllByTripIdOrderByDayAndSortOrder(1L);
    }

    private TripService service() {
        return new TripService(
                tripRepository,
                tripDayRepository,
                participantRepository,
                userRepository,
                itineraryItemRepository,
                voteOptionRepository,
                voteRepository
        );
    }
}
