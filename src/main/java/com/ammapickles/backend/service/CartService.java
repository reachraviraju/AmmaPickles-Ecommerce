package com.ammapickles.backend.service;

import com.ammapickles.backend.dto.cart.CartResponse;

public interface CartService {

    CartResponse getUserCart(Long userId);

    CartResponse addToCart(Long userId, Long productId, int quantity);

    CartResponse updateCartItem(Long cartItemId, int quantity);

    void removeCartItem(Long cartItemId);

    void clearCart(Long userId);
}