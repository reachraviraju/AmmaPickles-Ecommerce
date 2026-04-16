package com.ammapickles.backend.repository;

import com.ammapickles.backend.entity.Order;
import com.ammapickles.backend.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);
    Optional<Order> findByIdAndUserId(Long orderId, Long userId);
    long countByUserId(Long userId);
    long countByStatus(OrderStatus status);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    
    boolean existsByDeliveryAddressId(Long addressId);

    @Query("SELECT COALESCE(SUM(o.totalAmount + o.deliveryCharge), 0) FROM Order o WHERE o.status <> :status")
    BigDecimal getTotalRevenue(@Param("status") OrderStatus status);

    @Query("SELECT o.user.id, COUNT(o) FROM Order o WHERE o.user.id IN :userIds GROUP BY o.user.id")
    List<Object[]> countByUserIds(@Param("userIds") List<Long> userIds);
}
