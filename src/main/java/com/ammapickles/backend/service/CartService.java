package com.ammapickles.backend.service;

import com.ammapickles.backend.dto.cart.CartResponse;

public interface CartService {
	
	
    CartResponse getUserCart(Long userId);

    CartResponse addToCart(Long userId, Long productId, int quantity);

    

    

    void clearCart(Long userId);
    
    CartResponse updateCartItem(Long cartItemId, int quantity, Long requestingUserId);
	void removeCartItem(Long cartItemId, Long requestingUserId);

}