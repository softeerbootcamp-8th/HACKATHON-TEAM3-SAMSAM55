package com.samsam55.trip.trip.repository;

import com.samsam55.trip.trip.entity.Vote;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    @Modifying(flushAutomatically = true)
    @Query("""
            delete from Vote vote
            where vote.itineraryItem.tripDay.trip.id = :tripId
            """)
    int deleteAllByTripId(@Param("tripId") Long tripId);

    @Modifying(flushAutomatically = true)
    @Query("""
            delete from Vote vote
            where vote.itineraryItem.tripDay.id in :tripDayIds
            """)
    int deleteAllByTripDayIds(@Param("tripDayIds") Collection<Long> tripDayIds);
}
