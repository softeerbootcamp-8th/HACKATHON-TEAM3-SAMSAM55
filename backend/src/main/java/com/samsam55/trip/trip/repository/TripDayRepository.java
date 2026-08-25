package com.samsam55.trip.trip.repository;

import com.samsam55.trip.trip.entity.TripDay;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripDayRepository extends JpaRepository<TripDay, Long> {

    @Query("""
            select tripDay
            from TripDay tripDay
            where tripDay.trip.id = :tripId
            order by tripDay.dayNumber asc
            """)
    List<TripDay> findAllByTripIdOrderByDayNumberAsc(@Param("tripId") Long tripId);

    @Modifying(flushAutomatically = true)
    @Query("delete from TripDay day where day.trip.id = :tripId")
    int deleteAllByTripId(@Param("tripId") Long tripId);
}
