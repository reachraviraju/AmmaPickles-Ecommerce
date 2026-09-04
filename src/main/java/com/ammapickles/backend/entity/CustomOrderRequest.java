package com.ammapickles.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "custom_order_requests", indexes = {
    @Index(name = "idx_custom_order_status", columnList = "status"),
    @Index(name = "idx_custom_order_user", columnList = "user_id"),
    @Index(name = "idx_custom_order_date", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomOrderRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String phoneNumber;

    /** Main ingredient: Mango, Lemon, Ginger, etc. */
    @Column(nullable = false)
    private String pickleType;

    /** Preferred oil: Sesame, Mustard, Groundnut, etc. */
    @Column(nullable = false)
    private String oilPreference;

    /** Spice level: Mild, Medium, Hot, Extra Hot */
    @Column(nullable = false)
    private String spiceLevel;

    /** Salt level: Low, Medium, High */
    @Column(nullable = false)
    private String saltLevel;

    /** Additional ingredients requested */
    @Column(columnDefinition = "TEXT")
    private String additionalIngredients;

    /** Any other special requests */
    @Column(columnDefinition = "TEXT")
    private String specialInstructions;

    @Column(nullable = false)
    private String quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CustomOrderStatus status = CustomOrderStatus.NEW;

    @Column(columnDefinition = "TEXT")
    private String adminNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** Unique session ID linking this request to its chat conversation */
    @Column(nullable = false)
    private String sessionId;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (status == null) status = CustomOrderStatus.NEW;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
