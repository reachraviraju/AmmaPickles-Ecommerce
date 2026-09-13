package com.ammapickles.backend.repository;

import com.ammapickles.backend.entity.CustomOrderRequest;
import com.ammapickles.backend.entity.CustomOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface CustomOrderRequestRepository extends JpaRepository<CustomOrderRequest, Long> {

    /**
     * Count custom orders placed by phone or user ID in the given time window (e.g. last 24h).
     */
    @Query("SELECT COUNT(c) FROM CustomOrderRequest c WHERE " +
           "((:phone IS NOT NULL AND :phone != '' AND c.phoneNumber = :phone) " +
           " OR (:userId IS NOT NULL AND c.user.id = :userId)) " +
           "AND c.createdAt >= :since")
    long countOrdersSince(@Param("phone") String phone,
                          @Param("userId") Long userId,
                          @Param("since") LocalDateTime since);

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

    /**
     * Only fetch active/confirmed custom orders for customer's Orders page.
     * Unconfirmed requests (NEW, CONTACTED) remain in discussion and are not shown as orders yet.
     */
    @Query("SELECT c FROM CustomOrderRequest c WHERE (c.user.id = :userId " +
           "OR (c.phoneNumber = :phone AND :phone IS NOT NULL AND :phone != '')) " +
           "AND c.status IN (com.ammapickles.backend.entity.CustomOrderStatus.CONFIRMED, " +
           "com.ammapickles.backend.entity.CustomOrderStatus.PREPARING, " +
           "com.ammapickles.backend.entity.CustomOrderStatus.SHIPPED, " +
           "com.ammapickles.backend.entity.CustomOrderStatus.DELIVERED, " +
           "com.ammapickles.backend.entity.CustomOrderStatus.COMPLETED) " +
           "ORDER BY c.createdAt DESC")
    List<CustomOrderRequest> findConfirmedByUserIdOrPhone(@Param("userId") Long userId,
                                                         @Param("phone") String phone);
}
