package com.ammapickles.backend.controller;

import com.ammapickles.backend.dto.cart.CartResponse;
import com.ammapickles.backend.dto.common.ApiResponse;
import com.ammapickles.backend.security.CustomUserDetails;
import com.ammapickles.backend.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<CartResponse>> getUserCart(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (!userDetails.getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied"));
        }
        CartResponse response = cartService.getUserCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Cart fetched successfully", response));
    }

    @PostMapping("/user/{userId}/product/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @PathVariable Long userId,
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") int quantity,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (!userDetails.getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied"));
        }
        CartResponse response = cartService.addToCart(userId, productId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Product added to cart", response));
    }

    @PutMapping("/item/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @PathVariable Long cartItemId,
            @RequestParam int quantity,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CartResponse response = cartService.updateCartItem(cartItemId, quantity, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Cart item updated", response));
    }

    @DeleteMapping("/item/{cartItemId}")
    public ResponseEntity<ApiResponse<Void>> removeCartItem(
            @PathVariable Long cartItemId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        cartService.removeCartItem(cartItemId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart"));
    }

    @DeleteMapping("/user/{userId}/clear")
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (!userDetails.getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied"));
        }
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared successfully"));
    }
}