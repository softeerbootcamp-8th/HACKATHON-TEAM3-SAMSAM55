package com.samsam55.trip.trip.repository;

import com.samsam55.trip.trip.entity.Vote;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    Optional<Vote> findByItineraryItemIdAndParticipantId(Long itineraryItemId, Long participantId);

    long countByItineraryItemId(Long itineraryItemId);

    @Modifying(flushAutomatically = true)
    @Query("""
            delete from Vote vote
            where vote.itineraryItem.tripDay.trip.id = :tripId
            """)
    int deleteAllByTripId(@Param("tripId") Long tripId);
}
