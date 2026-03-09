package com.ammapickles.backend.dto.cart;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemResponse {

    private Long cartItemId;
    private Long productId;
    private Long variantId; 
    private String productName;
    private String sizeLabel;        // "1 kg"
    private BigDecimal price;        // price per unit
    private Integer quantity;
    private BigDecimal itemTotal;    // price × quantity
}