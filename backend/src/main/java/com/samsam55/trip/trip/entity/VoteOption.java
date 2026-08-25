package com.samsam55.trip.trip.entity;

import com.samsam55.trip.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "vote_option")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoteOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "itinerary_item_id", nullable = false)
    private ItineraryItem itineraryItem;

    @Column(length = 100, nullable = false)
    private String name;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "description_source", length = 20, nullable = false)
    private String descriptionSource;

    @Lob
    @Column(name = "image", columnDefinition = "LONGBLOB")
    private byte[] image;

    @Column(name = "image_content_type", length = 100)
    private String imageContentType;

    public VoteOption(
            ItineraryItem itineraryItem,
            String name,
            String description,
            String descriptionSource,
            byte[] image,
            String imageContentType
    ) {
        this.itineraryItem = itineraryItem;
        this.name = name;
        this.description = description;
        this.descriptionSource = descriptionSource;
        this.image = image;
        this.imageContentType = imageContentType;
    }
}
