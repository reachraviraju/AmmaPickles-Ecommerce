package com.ammapickles.backend.entity;

public enum CustomOrderStatus {
    NEW,
    CONTACTED,
    CONFIRMED,
    PREPARING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    @Deprecated
    COMPLETED
}
