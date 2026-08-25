package com.samsam55.trip.trip.entity;

import com.samsam55.trip.global.common.BaseEntity;
import com.samsam55.trip.member.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trip")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trip extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_user_id", nullable = false)
    private User hostUser;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "companion_count", nullable = false)
    private Integer companionCount;

    @Column(name = "invite_code", length = 64, nullable = false)
    private String inviteCode;

    public Trip(
            User hostUser,
            String title,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer companionCount,
            String inviteCode
    ) {
        this.hostUser = hostUser;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.companionCount = companionCount;
        this.inviteCode = inviteCode;
    }
}
