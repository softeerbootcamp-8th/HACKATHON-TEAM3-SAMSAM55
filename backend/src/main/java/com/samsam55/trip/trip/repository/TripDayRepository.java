package com.samsam55.trip.trip.repository;

import com.samsam55.trip.trip.entity.TripDay;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripDayRepository extends JpaRepository<TripDay, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TripDay t WHERE t.id = :id")
    Optional<TripDay> findByIdForUpdate(@Param("id") Long id);

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
