package com.samsam55.trip.trip.repository;

import com.samsam55.trip.trip.entity.ItineraryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItineraryItemRepository extends JpaRepository<ItineraryItem, Long> {

    @Query("SELECT COALESCE(MAX(i.sortOrder), 0) FROM ItineraryItem i WHERE i.tripDay.id = :tripDayId")
    int findMaxSortOrderByTripDayId(@Param("tripDayId") Long tripDayId);
}
