package com.ammapickles.backend.service;

import com.ammapickles.backend.dto.CartDTO;

public interface CartService {

    CartDTO getUserCart(Long userId);

    CartDTO addToCart(Long userId, Long productId, int quantity);

    CartDTO updateCartItem(Long cartItemId, int quantity);

    void removeCartItem(Long cartItemId);

    void clearCart(Long userId);
}
