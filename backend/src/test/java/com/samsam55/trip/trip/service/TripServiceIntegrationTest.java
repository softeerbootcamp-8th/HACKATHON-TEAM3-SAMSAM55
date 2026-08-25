package com.samsam55.trip.trip.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.samsam55.trip.member.entity.User;
import com.samsam55.trip.member.repository.UserRepository;
import com.samsam55.trip.trip.dto.TripCreateRequestDto;
import com.samsam55.trip.trip.dto.TripCreateResponseDto;
import com.samsam55.trip.trip.dto.TripDetailResponseDto;
import com.samsam55.trip.trip.entity.ItineraryItem;
import com.samsam55.trip.trip.entity.Trip;
import com.samsam55.trip.trip.entity.TripDay;
import com.samsam55.trip.trip.repository.ParticipantRepository;
import com.samsam55.trip.trip.repository.ItineraryItemRepository;
import com.samsam55.trip.trip.repository.TripDayRepository;
import com.samsam55.trip.trip.repository.TripRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TripServiceIntegrationTest {

    private final TripService tripService;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final TripDayRepository tripDayRepository;
    private final ParticipantRepository participantRepository;
    private final ItineraryItemRepository itineraryItemRepository;

    @Autowired
    TripServiceIntegrationTest(
            TripService tripService,
            UserRepository userRepository,
            TripRepository tripRepository,
            TripDayRepository tripDayRepository,
            ParticipantRepository participantRepository,
            ItineraryItemRepository itineraryItemRepository
    ) {
        this.tripService = tripService;
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.tripDayRepository = tripDayRepository;
        this.participantRepository = participantRepository;
        this.itineraryItemRepository = itineraryItemRepository;
    }

    @Test
    @DisplayName("여행 생성 시 여행 일차와 참여자 빈 슬롯을 함께 생성한다")
    void 여행_생성_시_여행_일차와_참여자_빈_슬롯을_함께_생성한다() {
        User host = userRepository.saveAndFlush(new User("trip-host", "hashed-password"));
        TripCreateRequestDto request = new TripCreateRequestDto(
                "제주 가족 여행",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 3),
                List.of("엄마", "아빠", "직접 입력 역할")
        );

        TripCreateResponseDto response = tripService.createTrip(host.getId(), request);

        assertThat(response.id()).isNotNull();
        assertThat(response.title()).isEqualTo("제주 가족 여행");
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 9, 3));
        assertThat(response.companionCount()).isEqualTo(3);
        assertThat(response.inviteCode()).isNotBlank().hasSize(32);
        assertThat(response.participants())
                .extracting("roleName")
                .containsExactly("엄마", "아빠", "직접 입력 역할");
        assertThat(response.participants())
                .allSatisfy(participant -> assertThat(participant.participantId()).isNotNull());

        assertThat(tripRepository.findById(response.id())).isPresent();
        assertThat(tripDayRepository.count()).isEqualTo(3);
        assertThat(participantRepository.count()).isEqualTo(3);
        assertThat(participantRepository.findAll())
                .allSatisfy(participant -> assertThat(participant.getJoinedAt()).isNull());
    }

    @Test
    @DisplayName("여행 상세 조회 시 날짜별 일정과 정렬된 일정 항목을 반환한다")
    void 여행_상세_조회_시_날짜별_일정과_정렬된_일정_항목을_반환한다() {
        User host = userRepository.saveAndFlush(new User("detail-host", "hashed-password"));
        Trip trip = tripRepository.saveAndFlush(new Trip(
                host,
                "도쿄 가족여행",
                LocalDateTime.of(2026, 9, 1, 0, 0),
                LocalDateTime.of(2026, 9, 4, 0, 0),
                2,
                "detail-invite-code"
        ));
        TripDay firstDay = tripDayRepository.saveAndFlush(
                new TripDay(trip, 1, LocalDate.of(2026, 9, 1))
        );
        TripDay secondDay = tripDayRepository.saveAndFlush(
                new TripDay(trip, 2, LocalDate.of(2026, 9, 2))
        );
        ItineraryItem secondDayItem = itineraryItemRepository.saveAndFlush(new ItineraryItem(
                secondDay,
                "숙소 체크인",
                "숙소",
                "VOTE",
                "PENDING",
                1,
                null
        ));
        ItineraryItem firstDayItem = itineraryItemRepository.saveAndFlush(new ItineraryItem(
                firstDay,
                "점심 식사",
                "식사",
                "VOTE",
                "VOTING",
                1,
                null
        ));

        TripDetailResponseDto response = tripService.findTrip(host.getId(), trip.getId());

        assertThat(response.id()).isEqualTo(trip.getId());
        assertThat(response.title()).isEqualTo("도쿄 가족여행");
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 9, 4));
        assertThat(response.companionCount()).isEqualTo(2);
        assertThat(response.days()).hasSize(2);
        assertThat(response.days().get(0).dayNumber()).isEqualTo(1);
        assertThat(response.days().get(0).date()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(response.days().get(0).items()).extracting("name")
                .containsExactly("점심 식사");
        assertThat(response.days().get(0).items().getFirst().id()).isEqualTo(firstDayItem.getId());
        assertThat(response.days().get(0).items().getFirst().category()).isEqualTo("식사");
        assertThat(response.days().get(0).items().getFirst().status()).isEqualTo("VOTING");
        assertThat(response.days().get(1).date()).isEqualTo(LocalDate.of(2026, 9, 2));
        assertThat(response.days().get(1).items()).extracting("name")
                .containsExactly("숙소 체크인");
        assertThat(response.days().get(1).items().getFirst().id()).isEqualTo(secondDayItem.getId());
    }
}
