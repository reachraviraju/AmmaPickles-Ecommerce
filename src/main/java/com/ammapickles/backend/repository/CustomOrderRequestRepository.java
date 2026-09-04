package com.ammapickles.backend.repository;

import com.ammapickles.backend.entity.CustomOrderRequest;
import com.ammapickles.backend.entity.CustomOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CustomOrderRequestRepository extends JpaRepository<CustomOrderRequest, Long> {

    List<CustomOrderRequest> findAllByOrderByCreatedAtDesc();

    List<CustomOrderRequest> findByStatusOrderByCreatedAtDesc(CustomOrderStatus status);

    List<CustomOrderRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByStatus(CustomOrderStatus status);

    CustomOrderRequest findBySessionId(String sessionId);
}
