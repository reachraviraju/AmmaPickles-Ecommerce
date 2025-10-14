package com.ammapickles.backend.service;

import com.ammapickles.backend.dto.CartDTO;
import com.ammapickles.backend.dto.CartItemDTO;

import java.util.List;

public interface CartService {
	
	 
	 List<CartItemDTO> getCartItems(Long userId);
	  CartItemDTO addToCart(Long userId, Long productId, int quantity);
	  
	    CartItemDTO updateCartItem(Long cartItemId, CartItemDTO cartItemDTO);
	    void removeCartItem(Long cartItemId);
	    void clearCart(Long userId);
	    
   
}
