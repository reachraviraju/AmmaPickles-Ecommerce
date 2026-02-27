package com.ammapickles.backend.repository;

import com.ammapickles.backend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Get all orders for a user
    List<Order> findByUserId(Long userId);

    // Get specific order AND verify it belongs to user
    Optional<Order> findByIdAndUserId(Long orderId, Long userId);
}
