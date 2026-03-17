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
    private BigDecimal grandTotal;
    private LocalDateTime orderDate;
    private String deliveryAddress;
    private List<OrderItemResponse> items;
}