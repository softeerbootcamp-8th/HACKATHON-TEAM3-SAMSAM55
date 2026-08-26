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
            select option.itineraryItem.id as itemId,
                   count(option) as optionCount
            from VoteOption option
            where option.itineraryItem.tripDay.trip.id = :tripId
            group by option.itineraryItem.id
            """)
    List<ItineraryItemOptionCount> countByTripId(@Param("tripId") Long tripId);

    interface ItineraryItemOptionCount {

        Long getItemId();

        long getOptionCount();
    }

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

    // clearAutomatically로 영속성 컨텍스트를 비운다 — 이 삭제 대상 중엔 이미 join fetch로
    // 로드돼 있던 확정 선택지(ItineraryItem.confirmedOption)가 섞여 있을 수 있는데,
    // 비우지 않으면 그 자바 객체가 캐시에 영속 상태로 남아있다가 뒤이은 ItineraryItem
    // 삭제 커밋 시점에 Hibernate가 유령 참조로 오인해 TransientPropertyValueException을 던진다.
    // 주의: clearAutomatically는 이 쿼리와 무관한 엔티티까지 포함해 트랜잭션의 영속성
    // 컨텍스트 전체를 비운다(현재 유일한 호출부인 ItineraryItemService.deleteItineraryItem은
    // 이 시점까지 itineraryItem·confirmedOption만 로드해서 문제없다). 이 메서드를 다른
    // 서비스 로직과 같은 트랜잭션에서 재사용하게 되면, 그 전후로 로드·수정 중이던 다른
    // 엔티티도 같이 detach되어 이후 변경사항이 조용히 저장되지 않을 수 있으니 주의한다.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from VoteOption option where option.itineraryItem.id = :itemId")
    int deleteAllByItineraryItemId(@Param("itemId") Long itemId);
}
