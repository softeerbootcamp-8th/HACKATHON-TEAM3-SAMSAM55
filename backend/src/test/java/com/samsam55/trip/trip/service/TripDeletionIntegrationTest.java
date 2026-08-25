package com.samsam55.trip.trip.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.samsam55.trip.member.entity.User;
import com.samsam55.trip.member.repository.UserRepository;
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
import com.samsam55.trip.global.support.AbstractMySqlContainerTest;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class TripDeletionIntegrationTest extends AbstractMySqlContainerTest {

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
    TripDeletionIntegrationTest(
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
    @DisplayName("여행 삭제 시 모든 하위 데이터를 함께 삭제한다")
    void 여행_삭제_시_모든_하위_데이터를_함께_삭제한다() {
        User host = userRepository.saveAndFlush(new User("delete-host", "hashed-password"));
        Trip trip = tripRepository.saveAndFlush(new Trip(
                host,
                "삭제할 여행",
                LocalDateTime.of(2026, 9, 1, 0, 0),
                LocalDateTime.of(2026, 9, 3, 0, 0),
                1,
                "delete-invite-code"
        ));
        TripDay tripDay = tripDayRepository.saveAndFlush(
                new TripDay(trip, 1, LocalDate.of(2026, 9, 1))
        );
        Participant participant = participantRepository.saveAndFlush(
                new Participant(trip, "엄마", null)
        );
        ItineraryItem itineraryItem = itineraryItemRepository.saveAndFlush(new ItineraryItem(
                tripDay,
                "오전 관광지",
                "TOURIST_SPOT",
                ItineraryItemDecisionType.VOTE,
                ItineraryItemStatus.PENDING,
                1,
                null
        ));
        VoteOption voteOption = voteOptionRepository.saveAndFlush(new VoteOption(
                itineraryItem,
                "성산일출봉",
                "일출 명소",
                "AI",
                null,
                null
        ));
        entityManager.createQuery("""
                update ItineraryItem item
                set item.confirmedOption = :option
                where item.id = :itemId
                """)
                .setParameter("option", voteOption)
                .setParameter("itemId", itineraryItem.getId())
                .executeUpdate();
        Vote vote = voteRepository.saveAndFlush(new Vote(voteOption, itineraryItem, participant));
        entityManager.clear();

        tripService.deleteTrip(host.getId(), trip.getId());

        assertThat(voteRepository.findById(vote.getId())).isEmpty();
        assertThat(voteOptionRepository.findById(voteOption.getId())).isEmpty();
        assertThat(itineraryItemRepository.findById(itineraryItem.getId())).isEmpty();
        assertThat(participantRepository.findById(participant.getId())).isEmpty();
        assertThat(tripDayRepository.findById(tripDay.getId())).isEmpty();
        assertThat(tripRepository.findById(trip.getId())).isEmpty();
    }
}
