package com.ammapickles.backend.repository;

import com.ammapickles.backend.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    //  Get all items for a specific order
    List<OrderItem> findByOrderId(Long orderId);

    // Get all items for a specific product (used in analytics/inventory)
    List<OrderItem> findByProductId(Long productId);
}
