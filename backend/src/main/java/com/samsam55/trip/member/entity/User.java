package com.samsam55.trip.member.entity;

import com.samsam55.trip.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", length = 100, nullable = false, unique = true)
    private String loginId;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    public User(String loginId, String passwordHash) {
        this.loginId = loginId;
        this.passwordHash = passwordHash;
    }
}
