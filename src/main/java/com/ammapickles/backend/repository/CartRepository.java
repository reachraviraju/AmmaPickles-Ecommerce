package com.ammapickles.backend.repository;

import com.ammapickles.backend.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    // Find cart by user - >  each user has exactly one cart
    Optional<Cart> findByUserId(Long userId);
}