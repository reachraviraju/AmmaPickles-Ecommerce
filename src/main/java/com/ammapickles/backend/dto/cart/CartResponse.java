package com.ammapickles.backend.dto.cart;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CartResponse {
    private Long cartId;
    private Long userId;
    private List<CartItemResponse> items;
    private int totalItems;
    private BigDecimal cartTotal;
    private boolean freeDelivery;
}