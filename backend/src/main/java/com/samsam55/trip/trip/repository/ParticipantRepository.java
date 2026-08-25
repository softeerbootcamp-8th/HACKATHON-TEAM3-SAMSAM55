package com.samsam55.trip.trip.repository;

import com.samsam55.trip.trip.entity.Participant;
import com.samsam55.trip.trip.entity.Trip;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    List<Participant> findAllByTripOrderById(Trip trip);

    Optional<Participant> findByIdAndTrip(Long id, Trip trip);

    long countByTripId(Long tripId);

    @Modifying(flushAutomatically = true)
    @Query("delete from Participant participant where participant.trip.id = :tripId")
    int deleteAllByTripId(@Param("tripId") Long tripId);
}
