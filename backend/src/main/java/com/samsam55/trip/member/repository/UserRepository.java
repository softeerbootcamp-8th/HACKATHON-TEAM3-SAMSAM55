package com.samsam55.trip.member.repository;

import com.samsam55.trip.member.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByLoginId(String loginId);

    Optional<User> findByLoginId(String loginId);
}
