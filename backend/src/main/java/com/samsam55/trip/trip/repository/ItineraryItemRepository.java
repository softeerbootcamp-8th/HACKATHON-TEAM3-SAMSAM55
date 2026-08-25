package com.samsam55.trip.trip.repository;

import com.samsam55.trip.trip.entity.ItineraryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItineraryItemRepository extends JpaRepository<ItineraryItem, Long> {

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
