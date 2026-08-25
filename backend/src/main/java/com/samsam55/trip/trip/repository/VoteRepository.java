package com.samsam55.trip.trip.repository;

import com.samsam55.trip.trip.entity.Vote;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    @Query("""
            select vote.itineraryItem.id as itemId,
                   count(distinct vote.participant.id) as votedCount
            from Vote vote
            where vote.itineraryItem.tripDay.trip.id = :tripId
              and vote.participant.trip.id = :tripId
            group by vote.itineraryItem.id
            """)
    List<ItineraryItemVoteCount> countDistinctParticipantsByTripId(@Param("tripId") Long tripId);

    interface ItineraryItemVoteCount {

        Long getItemId();

        long getVotedCount();
    }

    @Query("""
            select vote
            from Vote vote
            join fetch vote.option
            join fetch vote.participant
            where vote.itineraryItem.id = :itemId
            order by vote.id asc
            """)
    List<Vote> findAllByItineraryItemIdWithOptionAndParticipant(@Param("itemId") Long itemId);

    @Modifying(flushAutomatically = true)
    @Query("""
            delete from Vote vote
            where vote.itineraryItem.tripDay.trip.id = :tripId
            """)
    int deleteAllByTripId(@Param("tripId") Long tripId);
}
