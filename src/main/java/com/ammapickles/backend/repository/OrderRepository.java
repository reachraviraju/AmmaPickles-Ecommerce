package com.ammapickles.backend.repository;

import com.ammapickles.backend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    
    List<Order> findByUserId(Long userId);

   
    Optional<Order> findByIdAndUserId(Long id, Long userId);
}
