package com.samsam55.trip.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.member.repository.UserRepository;
import com.samsam55.trip.trip.dto.TripCreateRequestDto;
import com.samsam55.trip.trip.dto.TripListResponseDto;
import com.samsam55.trip.trip.dto.TripSummaryResponseDto;
import com.samsam55.trip.trip.entity.Trip;
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

    @Test
    @DisplayName("로그인한 사용자가 방장인 여행만 목록으로 반환한다")
    void 로그인한_사용자가_방장인_여행만_목록으로_반환한다() {
        when(tripRepository.findAllByHostUserIdOrderByIdAsc(1L)).thenReturn(List.of(trip));
        when(trip.getId()).thenReturn(1L);
        when(trip.getTitle()).thenReturn("제주 가족 여행");
        when(trip.getStartDate()).thenReturn(LocalDateTime.of(2026, 9, 1, 0, 0));
        when(trip.getEndDate()).thenReturn(LocalDateTime.of(2026, 9, 3, 0, 0));
        when(trip.getCompanionCount()).thenReturn(2);

        TripListResponseDto response = service().findTrips(1L);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().id()).isEqualTo(1L);
        assertThat(response.items().getFirst().title()).isEqualTo("제주 가족 여행");
        assertThat(response.items().getFirst().startDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(response.items().getFirst().endDate()).isEqualTo(LocalDate.of(2026, 9, 3));
        assertThat(response.items().getFirst().companionCount()).isEqualTo(2);
        verify(tripRepository).findAllByHostUserIdOrderByIdAsc(1L);
    }

    @Test
    @DisplayName("방장에게 여행이 없으면 빈 items를 반환한다")
    void 방장에게_여행이_없으면_빈_items를_반환한다() {
        when(tripRepository.findAllByHostUserIdOrderByIdAsc(1L)).thenReturn(List.of());

        TripListResponseDto response = service().findTrips(1L);

        assertThat(response.items()).isEmpty();
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

        TripSummaryResponseDto response = service().findTrip(1L, 1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("제주 가족 여행");
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 9, 3));
        assertThat(response.companionCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("방장이 아닌 사용자는 여행 상세 정보를 조회할 수 없다")
    void 방장이_아닌_사용자는_여행_상세_정보를_조회할_수_없다() {
        when(tripRepository.findByIdAndHostUserId(1L, 2L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service().findTrip(2L, 1L))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType().getCode()).isEqualTo("TRIP_NOT_FOUND"));
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
