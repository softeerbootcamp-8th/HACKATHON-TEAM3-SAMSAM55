package com.samsam55.trip.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.samsam55.trip.global.support.AbstractMySqlContainerTest;
import com.samsam55.trip.member.entity.User;
import com.samsam55.trip.member.repository.UserRepository;
import com.samsam55.trip.trip.entity.ItineraryItem;
import com.samsam55.trip.trip.entity.ItineraryItemDecisionType;
import com.samsam55.trip.trip.entity.ItineraryItemStatus;
import com.samsam55.trip.trip.entity.Trip;
import com.samsam55.trip.trip.entity.TripDay;
import com.samsam55.trip.trip.entity.VoteOption;
import com.samsam55.trip.trip.repository.ItineraryItemRepository;
import com.samsam55.trip.trip.repository.TripDayRepository;
import com.samsam55.trip.trip.repository.TripRepository;
import com.samsam55.trip.trip.repository.VoteOptionRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 확정된 일정 항목(confirmedOption이 걸려 join fetch로 로드되는 케이스)을 삭제해도
 * 실제 MySQL 위에서 예외 없이 지워지는지 확인한다. Mockito 단위 테스트는 실제 Hibernate
 * 영속성 컨텍스트/플러시 동작을 재현하지 못해 이 버그(TransientPropertyValueException)를
 * 잡지 못했다.
 */
@SpringBootTest
@Transactional
class ItineraryItemDeleteIntegrationTest extends AbstractMySqlContainerTest {

    private final ItineraryItemService itineraryItemService;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final TripDayRepository tripDayRepository;
    private final ItineraryItemRepository itineraryItemRepository;
    private final VoteOptionRepository voteOptionRepository;

    @Autowired
    ItineraryItemDeleteIntegrationTest(
            ItineraryItemService itineraryItemService,
            UserRepository userRepository,
            TripRepository tripRepository,
            TripDayRepository tripDayRepository,
            ItineraryItemRepository itineraryItemRepository,
            VoteOptionRepository voteOptionRepository
    ) {
        this.itineraryItemService = itineraryItemService;
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.tripDayRepository = tripDayRepository;
        this.itineraryItemRepository = itineraryItemRepository;
        this.voteOptionRepository = voteOptionRepository;
    }

    @Test
    @DisplayName("확정된 일정 항목을 삭제하면 예외 없이 항목과 선택지가 모두 지워진다")
    void 확정된_일정_항목을_삭제하면_예외_없이_항목과_선택지가_모두_지워진다() {
        User host = userRepository.saveAndFlush(new User("delete-host", "hashed-password"));
        Trip trip = tripRepository.saveAndFlush(new Trip(
                host, "제주 여행",
                LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 9, 3, 0, 0),
                1, "delete-invite-code"
        ));
        TripDay tripDay = tripDayRepository.saveAndFlush(new TripDay(trip, 1, LocalDate.of(2026, 9, 1)));
        ItineraryItem item = itineraryItemRepository.saveAndFlush(new ItineraryItem(
                tripDay, "점심 식사", "식사", ItineraryItemDecisionType.VOTE,
                ItineraryItemStatus.VOTING, 1, null
        ));
        VoteOption confirmedOption = voteOptionRepository.saveAndFlush(
                new VoteOption(item, "스시", "스시 설명", "HOST", null));
        VoteOption otherOption = voteOptionRepository.saveAndFlush(
                new VoteOption(item, "라멘", "라멘 설명", "HOST", null));
        item.confirm(confirmedOption);
        itineraryItemRepository.saveAndFlush(item);

        Long itemId = item.getId();

        assertThatCode(() -> itineraryItemService.deleteItineraryItem(host.getId(), itemId))
                .doesNotThrowAnyException();

        assertThat(itineraryItemRepository.findById(itemId)).isEmpty();
        assertThat(voteOptionRepository.findById(confirmedOption.getId())).isEmpty();
        assertThat(voteOptionRepository.findById(otherOption.getId())).isEmpty();
    }
}
