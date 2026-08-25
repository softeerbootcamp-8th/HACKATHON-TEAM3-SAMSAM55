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
import com.samsam55.trip.trip.entity.Trip;
import com.samsam55.trip.trip.repository.ParticipantRepository;
import com.samsam55.trip.trip.repository.TripDayRepository;
import com.samsam55.trip.trip.repository.TripRepository;
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
        verifyNoInteractions(userRepository, tripRepository, tripDayRepository, participantRepository);
    }

    private TripService service() {
        return new TripService(tripRepository, tripDayRepository, participantRepository, userRepository);
    }
}
