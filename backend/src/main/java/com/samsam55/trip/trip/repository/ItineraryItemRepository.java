package com.samsam55.trip.trip.repository;

import com.samsam55.trip.trip.entity.ItineraryItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItineraryItemRepository extends JpaRepository<ItineraryItem, Long> {

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
