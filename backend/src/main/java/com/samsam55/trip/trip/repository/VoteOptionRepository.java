package com.samsam55.trip.trip.repository;

import com.samsam55.trip.trip.entity.VoteOption;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoteOptionRepository extends JpaRepository<VoteOption, Long> {

    @Modifying(flushAutomatically = true)
    @Query("""
            delete from VoteOption option
            where option.itineraryItem.tripDay.trip.id = :tripId
            """)
    int deleteAllByTripId(@Param("tripId") Long tripId);

    @Modifying(flushAutomatically = true)
    @Query("""
            delete from VoteOption option
            where option.itineraryItem.tripDay.id in :tripDayIds
            """)
    int deleteAllByTripDayIds(@Param("tripDayIds") Collection<Long> tripDayIds);
}
