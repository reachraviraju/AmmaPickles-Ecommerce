package com.ammapickles.backend.dto.order;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemResponse {

    private Long productId;
    private String productName;
    private String sizeLabel;               // "1 kg", "2 kg"
    private Integer quantity;
    private BigDecimal priceAtTimeOfOrder;  // price when ordered — NOT current price!
    private BigDecimal itemTotal;           // quantity × price
}