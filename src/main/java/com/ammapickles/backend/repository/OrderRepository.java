package com.ammapickles.backend.repository;

import com.ammapickles.backend.entity.Order;
import com.ammapickles.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
}
