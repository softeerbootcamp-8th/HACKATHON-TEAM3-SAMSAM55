package com.samsam55.trip.trip.repository;

import com.samsam55.trip.trip.entity.Participant;
import com.samsam55.trip.trip.entity.Trip;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    List<Participant> findAllByTripOrderById(Trip trip);

    Optional<Participant> findByIdAndTrip(Long id, Trip trip);
}
