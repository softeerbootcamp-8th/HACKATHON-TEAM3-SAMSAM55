package com.samsam55.trip.trip.repository;

import com.samsam55.trip.trip.entity.ItineraryItem;
import com.samsam55.trip.trip.entity.ItineraryItemStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItineraryItemRepository extends JpaRepository<ItineraryItem, Long> {

    long countByTripDayTripId(Long tripId);

    List<ItineraryItem> findByTripDayIdOrderBySortOrderAsc(Long tripDayId);

    long countByTripDayTripIdAndStatus(Long tripId, ItineraryItemStatus status);

    @Query("SELECT COALESCE(MAX(i.sortOrder), 0) FROM ItineraryItem i WHERE i.tripDay.id = :tripDayId")
    int findMaxSortOrderByTripDayId(@Param("tripDayId") Long tripDayId);

    @Query("""
            select item
            from ItineraryItem item
            join fetch item.tripDay tripDay
            left join fetch item.confirmedOption
            where tripDay.trip.id = :tripId
            order by tripDay.dayNumber asc, item.sortOrder asc
            """)
    List<ItineraryItem> findAllByTripIdOrderByDayAndSortOrder(@Param("tripId") Long tripId);

    @Query("""
            select item
            from ItineraryItem item
            join item.tripDay tripDay
            where tripDay.trip.id = :tripId
              and item.status = com.samsam55.trip.trip.entity.ItineraryItemStatus.VOTING
              and not exists (
                  select 1
                  from Vote vote
                  where vote.itineraryItem = item
                    and vote.participant.id = :participantId
              )
            order by tripDay.dayNumber asc, item.sortOrder asc
            """)
    List<ItineraryItem> findUnvotedVotingItemsOrderByDayAndSortOrder(
            @Param("tripId") Long tripId, @Param("participantId") Long participantId);

    @Query("""
            select item
            from ItineraryItem item
            join fetch item.tripDay tripDay
            join fetch tripDay.trip trip
            join fetch trip.hostUser
            left join fetch item.confirmedOption
            where item.id = :itemId
            """)
    Optional<ItineraryItem> findByIdWithTripAndConfirmedOption(@Param("itemId") Long itemId);

    @Modifying(flushAutomatically = true)
    @Query("""
            update ItineraryItem item
            set item.confirmedOption = null
            where item.tripDay.trip.id = :tripId
            """)
    int clearConfirmedOptionByTripId(@Param("tripId") Long tripId);

    @Modifying(flushAutomatically = true)
    @Query("""
            delete from ItineraryItem item
            where item.tripDay.trip.id = :tripId
            """)
    int deleteAllByTripId(@Param("tripId") Long tripId);

    @Modifying(flushAutomatically = true)
    @Query("""
            update ItineraryItem item
            set item.confirmedOption = null
            where item.id = :itemId
            """)
    int clearConfirmedOptionByItemId(@Param("itemId") Long itemId);

    // (trip_day_id, sort_order) 유니크 제약이 있어 순서를 재배치할 때 중간에 값이 겹칠 수 있다.
    // 즉시 실행되는 벌크 UPDATE라 호출할 때마다 바로 DB에 반영되므로, 서비스에서 두 단계(임시 음수 값 →
    // 최종 값)로 나눠 호출하면 충돌 없이 안전하게 재배치할 수 있다. 벌크 업데이트는 영속성 컨텍스트를
    // 거치지 않고 SQL을 바로 실행하므로, 이미 로드돼 있던 엔티티가 옛날 sortOrder를 들고 있지 않도록
    // clearAutomatically로 영속성 컨텍스트를 비워 이후 조회가 항상 DB에서 새로 읽어오게 한다.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ItineraryItem item set item.sortOrder = :sortOrder where item.id = :itemId")
    void updateSortOrder(@Param("itemId") Long itemId, @Param("sortOrder") int sortOrder);
}
