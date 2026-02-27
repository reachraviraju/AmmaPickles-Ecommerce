package com.ammapickles.backend.dto.order;

import com.ammapickles.backend.entity.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponse {

    private Long id;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private BigDecimal deliveryCharge;
    private BigDecimal grandTotal;              // totalAmount + deliveryCharge
    private LocalDateTime orderDate;
    private String deliveryAddress;             // formatted: "street, city, district - pincode"
    private List<OrderItemResponse> items;
}