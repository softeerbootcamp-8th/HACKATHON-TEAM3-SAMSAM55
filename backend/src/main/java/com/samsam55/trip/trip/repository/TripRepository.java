package com.samsam55.trip.trip.repository;

import com.samsam55.trip.trip.entity.Trip;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<Trip, Long> {

    Optional<Trip> findByInviteCode(String inviteCode);
}
