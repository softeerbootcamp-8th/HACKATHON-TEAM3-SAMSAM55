package com.samsam55.trip.trip.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.samsam55.trip.member.entity.User;
import com.samsam55.trip.member.repository.UserRepository;
import com.samsam55.trip.trip.dto.TripSummaryResponseDto;
import com.samsam55.trip.trip.dto.TripUpdateRequestDto;
import com.samsam55.trip.trip.entity.ItineraryItem;
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
import jakarta.persistence.EntityManager;
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
class TripUpdateIntegrationTest {

    private final TripService tripService;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final TripDayRepository tripDayRepository;
    private final ParticipantRepository participantRepository;
    private final ItineraryItemRepository itineraryItemRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final VoteRepository voteRepository;
    private final EntityManager entityManager;

    @Autowired
    TripUpdateIntegrationTest(
            TripService tripService,
            UserRepository userRepository,
            TripRepository tripRepository,
            TripDayRepository tripDayRepository,
            ParticipantRepository participantRepository,
            ItineraryItemRepository itineraryItemRepository,
            VoteOptionRepository voteOptionRepository,
            VoteRepository voteRepository,
            EntityManager entityManager
    ) {
        this.tripService = tripService;
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.tripDayRepository = tripDayRepository;
        this.participantRepository = participantRepository;
        this.itineraryItemRepository = itineraryItemRepository;
        this.voteOptionRepository = voteOptionRepository;
        this.voteRepository = voteRepository;
        this.entityManager = entityManager;
    }

    @Test
    @DisplayName("여행 기간 축소 시 범위 밖 일정과 투표 데이터를 삭제하고 새 일차를 생성한다")
    void 여행_기간_축소_시_범위_밖_일정과_투표_데이터를_삭제하고_새_일차를_생성한다() {
        User host = userRepository.saveAndFlush(new User("update-host", "hashed-password"));
        Trip trip = tripRepository.saveAndFlush(new Trip(
                host,
                "도쿄 가족 여행",
                LocalDateTime.of(2026, 9, 1, 0, 0),
                LocalDateTime.of(2026, 9, 3, 0, 0),
                2,
                "update-invite-code"
        ));
        TripDay firstDay = tripDayRepository.saveAndFlush(
                new TripDay(trip, 1, LocalDate.of(2026, 9, 1))
        );
        TripDay secondDay = tripDayRepository.saveAndFlush(
                new TripDay(trip, 2, LocalDate.of(2026, 9, 2))
        );
        TripDay thirdDay = tripDayRepository.saveAndFlush(
                new TripDay(trip, 3, LocalDate.of(2026, 9, 3))
        );
        Participant participant = participantRepository.saveAndFlush(
                new Participant(trip, "엄마", null)
        );
        ItineraryItem removedItem = itineraryItemRepository.saveAndFlush(new ItineraryItem(
                firstDay,
                "삭제될 일정",
                "관광",
                "VOTE",
                "VOTING",
                1,
                null
        ));
        VoteOption removedOption = voteOptionRepository.saveAndFlush(new VoteOption(
                removedItem,
                "삭제될 선택지",
                "설명",
                "USER",
                null
        ));
        entityManager.createQuery("""
                update ItineraryItem item
                set item.confirmedOption = :option
                where item.id = :itemId
                """)
                .setParameter("option", removedOption)
                .setParameter("itemId", removedItem.getId())
                .executeUpdate();
        Vote removedVote = voteRepository.saveAndFlush(new Vote(removedOption, removedItem, participant));
        ItineraryItem retainedItem = itineraryItemRepository.saveAndFlush(new ItineraryItem(
                secondDay,
                "유지할 일정",
                "식사",
                "VOTE",
                "PENDING",
                1,
                null
        ));
        entityManager.clear();

        TripSummaryResponseDto response = tripService.updateTrip(host.getId(), trip.getId(), new TripUpdateRequestDto(
                "도쿄 효도 여행",
                LocalDate.of(2026, 9, 2),
                LocalDate.of(2026, 9, 4)
        ));

        assertThat(response.id()).isEqualTo(trip.getId());
        assertThat(response.title()).isEqualTo("도쿄 효도 여행");
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 9, 2));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 9, 4));
        assertThat(response.companionCount()).isEqualTo(2);
        assertThat(tripRepository.findById(trip.getId()))
                .get()
                .satisfies(updatedTrip -> {
                    assertThat(updatedTrip.getTitle()).isEqualTo("도쿄 효도 여행");
                    assertThat(updatedTrip.getStartDate().toLocalDate()).isEqualTo(LocalDate.of(2026, 9, 2));
                    assertThat(updatedTrip.getEndDate().toLocalDate()).isEqualTo(LocalDate.of(2026, 9, 4));
                });
        assertThat(voteRepository.findById(removedVote.getId())).isEmpty();
        assertThat(voteOptionRepository.findById(removedOption.getId())).isEmpty();
        assertThat(itineraryItemRepository.findById(removedItem.getId())).isEmpty();
        assertThat(itineraryItemRepository.findById(retainedItem.getId())).isPresent();
        assertThat(tripDayRepository.findAllByTripIdOrderByDayNumberAsc(trip.getId()))
                .extracting(TripDay::getTripDate)
                .containsExactly(
                        LocalDate.of(2026, 9, 2),
                        LocalDate.of(2026, 9, 3),
                        LocalDate.of(2026, 9, 4)
                );
        assertThat(tripDayRepository.findAllByTripIdOrderByDayNumberAsc(trip.getId()).getFirst().getId())
                .isEqualTo(secondDay.getId());
        assertThat(tripDayRepository.findAllByTripIdOrderByDayNumberAsc(trip.getId()).get(1).getId())
                .isEqualTo(thirdDay.getId());
    }
}
