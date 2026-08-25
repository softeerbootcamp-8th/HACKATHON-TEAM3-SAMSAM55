package com.samsam55.trip.trip.repository;

import com.samsam55.trip.trip.entity.Vote;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    @Query("""
            select vote
            from Vote vote
            join fetch vote.itineraryItem
            join fetch vote.option
            join fetch vote.participant
            where vote.itineraryItem.tripDay.trip.id = :tripId
            order by vote.id asc
            """)
    List<Vote> findAllByTripIdWithOptionAndParticipant(@Param("tripId") Long tripId);

    @Modifying(flushAutomatically = true)
    @Query("""
            delete from Vote vote
            where vote.itineraryItem.tripDay.trip.id = :tripId
            """)
    int deleteAllByTripId(@Param("tripId") Long tripId);
}
