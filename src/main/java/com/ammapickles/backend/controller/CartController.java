package com.ammapickles.backend.controller;

import com.ammapickles.backend.dto.cart.CartResponse;
import com.ammapickles.backend.dto.common.ApiResponse;
import com.ammapickles.backend.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

  
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<CartResponse>> getUserCart(
            @PathVariable Long userId) {

        CartResponse response = cartService.getUserCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Cart fetched successfully", response));
    }

    // POST /api/cart/user/{userId}/product/{productId}?quantity=2
    @PostMapping("/user/{userId}/product/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @PathVariable Long userId,
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") int quantity) {

        CartResponse response = cartService.addToCart(userId, productId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Product added to cart", response));
    }

    // PUT /api/cart/item/{cartItemId}?quantity=3
    @PutMapping("/item/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @PathVariable Long cartItemId,
            @RequestParam int quantity) {

        CartResponse response = cartService.updateCartItem(cartItemId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Cart item updated", response));
    }

  
    @DeleteMapping("/item/{cartItemId}")
    public ResponseEntity<ApiResponse<Void>> removeCartItem(
            @PathVariable Long cartItemId) {

        cartService.removeCartItem(cartItemId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart"));
    }

   
    @DeleteMapping("/user/{userId}/clear")
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @PathVariable Long userId) {

        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared successfully"));
    }
}