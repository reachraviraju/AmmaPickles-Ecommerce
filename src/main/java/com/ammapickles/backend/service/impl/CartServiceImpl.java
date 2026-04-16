package com.ammapickles.backend.service.impl;

import com.ammapickles.backend.dto.cart.CartItemResponse;
import com.ammapickles.backend.dto.cart.CartResponse;
import com.ammapickles.backend.entity.Cart;
import com.ammapickles.backend.entity.CartItem;
import com.ammapickles.backend.entity.Product;
import com.ammapickles.backend.entity.User;
import com.ammapickles.backend.exception.ResourceNotFoundException;
import com.ammapickles.backend.repository.CartItemRepository;
import com.ammapickles.backend.repository.CartRepository;
import com.ammapickles.backend.repository.OrderRepository;
import com.ammapickles.backend.repository.ProductRepository;
import com.ammapickles.backend.repository.UserRepository;
import com.ammapickles.backend.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    private static final BigDecimal FREE_DELIVERY_ABOVE   = BigDecimal.valueOf(1000);
    private static final BigDecimal FIRST_ORDER_FREE_ABOVE = BigDecimal.valueOf(500);

    @Override
    @Transactional(readOnly = true)
    public CartResponse getUserCart(Long userId) {
        log.info("Fetching cart for user: {}", userId);
        Cart cart = getOrCreateCart(userId);
        return mapToResponse(cart, userId);
    }

    @Override
    @Transactional
    public CartResponse addToCart(Long userId, Long productId, int quantity) {
        log.info("Adding product {} to cart for user {}", productId, userId);

        if (quantity <= 0)
            throw new IllegalArgumentException("Quantity must be at least 1");

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        if (!product.isInStock())
            throw new IllegalStateException("Product is out of stock: " + product.getName());

        if (product.getQuantity() < quantity)
            throw new IllegalStateException("Insufficient stock. Available: " + product.getQuantity());

        Cart cart = getOrCreateCart(userId);

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int updatedQuantity = item.getQuantity() + quantity;
            if (product.getQuantity() < updatedQuantity) {
                throw new IllegalStateException("Insufficient stock. Available: " + product.getQuantity());
            }
            item.setQuantity(updatedQuantity);
            log.info("Updated quantity for product {} in cart", productId);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .build();
            cart.getItems().add(newItem);
            log.info("Added new product {} to cart", productId);
        }

        Cart saved = cartRepository.save(cart);
        return mapToResponse(saved, userId);
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(Long cartItemId, int quantity, Long requestingUserId) {
        log.info("Updating cart item {} to quantity {}", cartItemId, quantity);

        if (quantity <= 0)
            throw new IllegalArgumentException("Quantity must be at least 1");

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + cartItemId));

        if (!item.getCart().getUser().getId().equals(requestingUserId))
            throw new AccessDeniedException("Access denied to cart item: " + cartItemId);

        if (item.getProduct().getQuantity() < quantity)
            throw new IllegalStateException("Insufficient stock. Available: " + item.getProduct().getQuantity());

        item.setQuantity(quantity);
        cartItemRepository.save(item);
        return mapToResponse(item.getCart(), requestingUserId);
    }

    @Override
    @Transactional
    public void removeCartItem(Long cartItemId, Long requestingUserId) {
        log.info("Removing cart item: {}", cartItemId);

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + cartItemId));

        if (!item.getCart().getUser().getId().equals(requestingUserId))
            throw new AccessDeniedException("Access denied to cart item: " + cartItemId);

        cartItemRepository.delete(item);
        log.info("Cart item removed: {}", cartItemId);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        log.info("Clearing cart for user: {}", userId);

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));

        cart.getItems().clear();
        cartRepository.save(cart);
        log.info("Cart cleared for user: {}", userId);
    }

    // ── HELPERS ──────────────────────────────────────────────

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("No cart found for user {} — creating new cart", userId);
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
                    return cartRepository.save(Cart.builder().user(user).build());
                });
    }

    private boolean calculateFreeDelivery(BigDecimal total, Long userId) {
        if (total.compareTo(FREE_DELIVERY_ABOVE) >= 0) return true;
        boolean isFirstOrder = orderRepository.countByUserId(userId) == 0;
        return isFirstOrder && total.compareTo(FIRST_ORDER_FREE_ABOVE) >= 0;
    }

    private CartResponse mapToResponse(Cart cart, Long userId) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(item -> {
                    CartItemResponse r = new CartItemResponse();
                    r.setCartItemId(item.getId());
                    r.setProductId(item.getProduct().getId());
                    r.setVariantId(item.getProduct().getId());
                    r.setProductName(item.getProduct().getName());
                    r.setPrice(item.getProduct().getPrice());
                    r.setQuantity(item.getQuantity());
                    r.setItemTotal(item.getProduct().getPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity())));
                    if (item.getProduct().getSize() != null)
                        r.setSizeLabel(item.getProduct().getSize().getLabel());
                    return r;
                })
                .toList();

        BigDecimal total = cart.getCartTotal();
        boolean freeDelivery = calculateFreeDelivery(total, userId);

        CartResponse response = new CartResponse();
        response.setCartId(cart.getId());
        response.setUserId(cart.getUser().getId());
        response.setItems(itemResponses);
        response.setTotalItems(cart.getItems().size());
        response.setCartTotal(total);
        response.setFreeDelivery(freeDelivery);
        return response;
    }
}
