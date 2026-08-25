package com.samsam55.trip.trip.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.samsam55.trip.member.entity.User;
import com.samsam55.trip.member.repository.UserRepository;
import com.samsam55.trip.trip.dto.TripCreateRequestDto;
import com.samsam55.trip.trip.dto.TripCreateResponseDto;
import com.samsam55.trip.trip.repository.ParticipantRepository;
import com.samsam55.trip.trip.repository.TripDayRepository;
import com.samsam55.trip.trip.repository.TripRepository;
import java.time.LocalDate;
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

    @Autowired
    TripServiceIntegrationTest(
            TripService tripService,
            UserRepository userRepository,
            TripRepository tripRepository,
            TripDayRepository tripDayRepository,
            ParticipantRepository participantRepository
    ) {
        this.tripService = tripService;
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.tripDayRepository = tripDayRepository;
        this.participantRepository = participantRepository;
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
}
