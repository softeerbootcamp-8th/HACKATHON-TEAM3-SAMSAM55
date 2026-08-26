package com.samsam55.trip.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.global.support.AbstractMySqlContainerTest;
import com.samsam55.trip.member.entity.User;
import com.samsam55.trip.member.repository.UserRepository;
import com.samsam55.trip.trip.entity.ItineraryItem;
import com.samsam55.trip.trip.entity.ItineraryItemDecisionType;
import com.samsam55.trip.trip.entity.ItineraryItemStatus;
import com.samsam55.trip.trip.entity.Trip;
import com.samsam55.trip.trip.entity.TripDay;
import com.samsam55.trip.trip.exception.TripErrorType;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * (trip_day_id, sort_order) 유니크 제약이 실제 MySQL에서도 재배치 중 충돌 없이 동작하는지 확인한다.
 * Mockito 단위 테스트로는 실제 제약 위반 여부를 검증할 수 없어 Testcontainers로 따로 확인한다.
 */
@SpringBootTest
@Transactional
class ItineraryItemReorderIntegrationTest extends AbstractMySqlContainerTest {

    private final ItineraryItemService itineraryItemService;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final TripDayRepository tripDayRepository;
    private final ItineraryItemRepository itineraryItemRepository;

    @Autowired
    ItineraryItemReorderIntegrationTest(
            ItineraryItemService itineraryItemService,
            UserRepository userRepository,
            TripRepository tripRepository,
            TripDayRepository tripDayRepository,
            ItineraryItemRepository itineraryItemRepository
    ) {
        this.itineraryItemService = itineraryItemService;
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.tripDayRepository = tripDayRepository;
        this.itineraryItemRepository = itineraryItemRepository;
    }

    @Test
    @DisplayName("순서를 재배치해도 유니크 제약 위반 없이 새 순서가 반영된다")
    void 순서를_재배치해도_유니크_제약_위반_없이_새_순서가_반영된다() {
        User host = userRepository.saveAndFlush(new User("reorder-host", "hashed-password"));
        Trip trip = tripRepository.saveAndFlush(new Trip(
                host, "제주 여행",
                LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 9, 3, 0, 0),
                1, "reorder-invite-code"
        ));
        TripDay tripDay = tripDayRepository.saveAndFlush(new TripDay(trip, 1, LocalDate.of(2026, 9, 1)));
        ItineraryItem first = itineraryItemRepository.saveAndFlush(
                new ItineraryItem(tripDay, "아침", "식사", ItineraryItemDecisionType.VOTE, ItineraryItemStatus.PENDING, 1, null));
        ItineraryItem second = itineraryItemRepository.saveAndFlush(
                new ItineraryItem(tripDay, "관광", "관광", ItineraryItemDecisionType.VOTE, ItineraryItemStatus.PENDING, 2, null));
        ItineraryItem third = itineraryItemRepository.saveAndFlush(
                new ItineraryItem(tripDay, "저녁", "식사", ItineraryItemDecisionType.VOTE, ItineraryItemStatus.PENDING, 3, null));

        itineraryItemService.reorderItineraryItems(
                host.getId(), tripDay.getId(), List.of(third.getId(), first.getId(), second.getId()));

        List<ItineraryItem> reordered = itineraryItemRepository.findByTripDayIdOrderBySortOrderAsc(tripDay.getId());
        assertThat(reordered).extracting(ItineraryItem::getId)
                .containsExactly(third.getId(), first.getId(), second.getId());
        assertThat(reordered).extracting(ItineraryItem::getSortOrder).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("일부 항목이 빠진 목록으로 재배치를 시도하면 예외가 발생하고 기존 순서는 그대로다")
    void 일부_항목이_빠진_목록으로_재배치를_시도하면_예외가_발생하고_기존_순서는_그대로다() {
        User host = userRepository.saveAndFlush(new User("reorder-host-2", "hashed-password"));
        Trip trip = tripRepository.saveAndFlush(new Trip(
                host, "제주 여행",
                LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 9, 3, 0, 0),
                1, "reorder-invite-code-2"
        ));
        TripDay tripDay = tripDayRepository.saveAndFlush(new TripDay(trip, 1, LocalDate.of(2026, 9, 1)));
        ItineraryItem first = itineraryItemRepository.saveAndFlush(
                new ItineraryItem(tripDay, "아침", "식사", ItineraryItemDecisionType.VOTE, ItineraryItemStatus.PENDING, 1, null));
        ItineraryItem second = itineraryItemRepository.saveAndFlush(
                new ItineraryItem(tripDay, "관광", "관광", ItineraryItemDecisionType.VOTE, ItineraryItemStatus.PENDING, 2, null));

        assertThatThrownBy(() -> itineraryItemService.reorderItineraryItems(
                host.getId(), tripDay.getId(), List.of(first.getId())))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.ITINERARY_ITEM_ORDER_MISMATCH));

        List<ItineraryItem> unchanged = itineraryItemRepository.findByTripDayIdOrderBySortOrderAsc(tripDay.getId());
        assertThat(unchanged).extracting(ItineraryItem::getId).containsExactly(first.getId(), second.getId());
    }
}
