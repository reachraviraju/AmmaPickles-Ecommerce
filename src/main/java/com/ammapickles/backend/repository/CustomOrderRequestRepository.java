package com.ammapickles.backend.repository;

import com.ammapickles.backend.entity.CustomOrderRequest;
import com.ammapickles.backend.entity.CustomOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CustomOrderRequestRepository extends JpaRepository<CustomOrderRequest, Long> {

    List<CustomOrderRequest> findAllByOrderByCreatedAtDesc();

    List<CustomOrderRequest> findByStatusOrderByCreatedAtDesc(CustomOrderStatus status);

    List<CustomOrderRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByStatus(CustomOrderStatus status);

    CustomOrderRequest findBySessionId(String sessionId);

    /**
     * Find custom orders belonging to a user (by user_id or matching phone number).
     * This ensures custom orders placed before login (linked by phone) are also visible.
     */
    @Query("SELECT c FROM CustomOrderRequest c WHERE c.user.id = :userId " +
           "OR (c.phoneNumber = :phone AND :phone IS NOT NULL AND :phone != '') " +
           "ORDER BY c.createdAt DESC")
    List<CustomOrderRequest> findByUserIdOrPhone(@Param("userId") Long userId,
                                                  @Param("phone") String phone);
}
