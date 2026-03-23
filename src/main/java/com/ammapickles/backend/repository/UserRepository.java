package com.ammapickles.backend.repository;

import com.ammapickles.backend.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    // Count users registered after a given time (for today / this week)
    long countByCreatedAtAfter(LocalDateTime dateTime);

    // Recent users ordered by join date — for admin dashboard table
    List<User> findAllByOrderByCreatedAtDesc(Pageable pageable);
}