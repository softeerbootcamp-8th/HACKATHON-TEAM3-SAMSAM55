package com.samsam55.trip.trip.repository;

import com.samsam55.trip.trip.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    @Modifying(flushAutomatically = true)
    @Query("delete from Participant participant where participant.trip.id = :tripId")
    int deleteAllByTripId(@Param("tripId") Long tripId);
}
