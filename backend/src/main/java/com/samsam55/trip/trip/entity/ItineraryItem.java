package com.samsam55.trip.trip.entity;

import com.samsam55.trip.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "itinerary_item", uniqueConstraints = @UniqueConstraint(
        name = "uk_itinerary_item_trip_day_sort_order", columnNames = {"trip_day_id", "sort_order"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItineraryItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_day_id", nullable = false)
    private TripDay tripDay;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 50)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_type", length = 20, nullable = false)
    private ItineraryItemDecisionType decisionType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ItineraryItemStatus status;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_option_id")
    private VoteOption confirmedOption;

    public ItineraryItem(
            TripDay tripDay,
            String name,
            String category,
            ItineraryItemDecisionType decisionType,
            ItineraryItemStatus status,
            Integer sortOrder,
            VoteOption confirmedOption
    ) {
        this.tripDay = tripDay;
        this.name = name;
        this.category = category;
        this.decisionType = decisionType;
        this.status = status;
        this.sortOrder = sortOrder;
        this.confirmedOption = confirmedOption;
    }

    public void openVote() {
        this.status = ItineraryItemStatus.VOTING;
    }

    public void confirm(VoteOption option) {
        this.status = ItineraryItemStatus.CONFIRMED;
        this.confirmedOption = option;
    }

    public void unconfirm() {
        this.status = ItineraryItemStatus.VOTING;
        this.confirmedOption = null;
    }
}
