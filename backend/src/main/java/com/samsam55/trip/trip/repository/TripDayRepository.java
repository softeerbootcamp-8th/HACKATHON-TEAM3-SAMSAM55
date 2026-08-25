package com.samsam55.trip.trip.repository;

import com.samsam55.trip.trip.entity.TripDay;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripDayRepository extends JpaRepository<TripDay, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TripDay t WHERE t.id = :id")
    Optional<TripDay> findByIdForUpdate(@Param("id") Long id);
}
