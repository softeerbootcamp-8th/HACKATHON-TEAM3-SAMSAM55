package com.samsam55.trip.trip.repository;

import com.samsam55.trip.trip.entity.TripDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripDayRepository extends JpaRepository<TripDay, Long> {

    @Modifying(flushAutomatically = true)
    @Query("delete from TripDay day where day.trip.id = :tripId")
    int deleteAllByTripId(@Param("tripId") Long tripId);
}
