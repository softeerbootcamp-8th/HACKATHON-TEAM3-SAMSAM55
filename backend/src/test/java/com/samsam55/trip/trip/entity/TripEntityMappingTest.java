package com.samsam55.trip.trip.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.samsam55.trip.member.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
class TripEntityMappingTest {

    private final EntityManager entityManager;

    @Autowired
    TripEntityMappingTest(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Test
    @DisplayName("ERD 엔티티와 연관관계를 저장할 수 있다")
    @Transactional
    void ERD_엔티티와_연관관계를_저장할_수_있다() {
        User user = new User("host", "hashed-password");
        entityManager.persist(user);

        Trip trip = new Trip(
                user,
                "제주 여행",
                LocalDateTime.of(2026, 9, 1, 9, 0),
                LocalDateTime.of(2026, 9, 3, 18, 0),
                3,
                "invite-code"
        );
        entityManager.persist(trip);

        TripDay tripDay = new TripDay(trip, 1, LocalDate.of(2026, 9, 1));
        entityManager.persist(tripDay);

        Participant participant = new Participant(trip, "PARENT", null);
        entityManager.persist(participant);

        ItineraryItem itineraryItem = new ItineraryItem(
                tripDay,
                "오전 관광지",
                "TOURIST_SPOT",
                "VOTE",
                "PENDING",
                1,
                null
        );
        entityManager.persist(itineraryItem);

        VoteOption voteOption = new VoteOption(
                itineraryItem,
                "성산일출봉",
                "일출 명소",
                "AI",
                null
        );
        entityManager.persist(voteOption);

        Vote vote = new Vote(voteOption, itineraryItem, participant);
        entityManager.persist(vote);
        entityManager.flush();

        assertThat(user.getId()).isNotNull();
        assertThat(trip.getId()).isNotNull();
        assertThat(tripDay.getId()).isNotNull();
        assertThat(participant.getId()).isNotNull();
        assertThat(itineraryItem.getId()).isNotNull();
        assertThat(voteOption.getId()).isNotNull();
        assertThat(vote.getId()).isNotNull();
        assertThat(trip.getCreatedAt()).isNotNull();
        assertThat(voteOption.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("사용자 login_id는 중복될 수 없다")
    @Transactional
    void 사용자_login_id는_중복될_수_없다() {
        entityManager.persist(new User("duplicate-login-id", "password-1"));

        assertThrows(PersistenceException.class, () -> {
            entityManager.persist(new User("duplicate-login-id", "password-2"));
            entityManager.flush();
        });
    }
}
