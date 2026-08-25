package com.samsam55.trip.trip.repository;

import com.samsam55.trip.trip.entity.ItineraryItem;
import com.samsam55.trip.trip.entity.VoteOption;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoteOptionRepository extends JpaRepository<VoteOption, Long> {

    int countByItineraryItemId(Long itineraryItemId);

    Optional<VoteOption> findByIdAndItineraryItemId(Long id, Long itineraryItemId);

    List<VoteOption> findByItineraryItem(ItineraryItem itineraryItem);

    long countByItineraryItem(ItineraryItem itineraryItem);

    List<VoteOption> findAllByItineraryItemIdOrderByIdAsc(Long itineraryItemId);

    @Query("""
            select option
            from VoteOption option
            join fetch option.itineraryItem item
            join fetch item.tripDay tripDay
            join fetch tripDay.trip trip
            join fetch trip.hostUser
            where option.id = :optionId
            """)
    Optional<VoteOption> findByIdWithTrip(@Param("optionId") Long optionId);

    @Modifying(flushAutomatically = true)
    @Query("""
            delete from VoteOption option
            where option.itineraryItem.tripDay.trip.id = :tripId
            """)
    int deleteAllByTripId(@Param("tripId") Long tripId);
}
