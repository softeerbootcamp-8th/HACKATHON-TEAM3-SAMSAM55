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
}
