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
import com.ammapickles.backend.repository.ProductRepository;
import com.ammapickles.backend.repository.UserRepository;
import com.ammapickles.backend.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // GET CART

    @Override
    @Transactional(readOnly = true)
    public CartResponse getUserCart(Long userId) {
        log.info("Fetching cart for user: {}", userId);

        Cart cart = getOrCreateCart(userId);
        return mapToResponse(cart);
    }

    // ADD TO CART 

    @Override
    @Transactional
    public CartResponse addToCart(Long userId, Long productId, int quantity) {
        log.info("Adding product {} to cart for user {}", productId, userId);

        // Validate quantity
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }

        // Load product and check stock
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        if (!product.isInStock()) {
            throw new IllegalStateException("Product is out of stock: " + product.getName());
        }

        if (product.getQuantity() < quantity) {
            throw new IllegalStateException(
                    "Insufficient stock. Available: " + product.getQuantity());
        }

        // Get or create cart
        Cart cart = getOrCreateCart(userId);

        // Check if product already exists in cart
        // If yes → update quantity, if no -> add new item
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            // Product already in cart —>  just increase quantity
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            log.info("Updated quantity for product {} in cart", productId);
        } else {
            // New product —> add to cart
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .build();
            cart.getItems().add(newItem);
            log.info("Added new product {} to cart", productId);
        }

        Cart saved = cartRepository.save(cart);
        return mapToResponse(saved);
    }

    // UPDATE CART ITEM 

    @Override
    @Transactional
    public CartResponse updateCartItem(Long cartItemId, int quantity) {
        log.info("Updating cart item {} to quantity {}", cartItemId, quantity);

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + cartItemId));

        // Check stock availability
        if (item.getProduct().getQuantity() < quantity) {
            throw new IllegalStateException(
                    "Insufficient stock. Available: " + item.getProduct().getQuantity());
        }

        item.setQuantity(quantity);
        // Dirty checking handles save

        return mapToResponse(item.getCart());
    }

    // REMOVE CART ITEM 

    @Override
    @Transactional
    public void removeCartItem(Long cartItemId) {
        log.info("Removing cart item: {}", cartItemId);

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + cartItemId));

        cartItemRepository.delete(item);
        log.info("Cart item removed: {}", cartItemId);
    }

    // CLEAR CART

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

    // PRIVATE HELPERS 

    // Get existing cart or create new one if not exists
    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("No cart found for user {} — creating new cart", userId);

                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

                    Cart newCart = Cart.builder()
                            .user(user)
                            .build();

                    return cartRepository.save(newCart);
                });
    }

    private CartResponse mapToResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(item -> {
                    CartItemResponse itemResponse = new CartItemResponse();
                    itemResponse.setCartItemId(item.getId());
                    itemResponse.setProductId(item.getProduct().getId());
                    itemResponse.setProductName(item.getProduct().getName());
                    itemResponse.setPrice(item.getProduct().getPrice());
                    itemResponse.setQuantity(item.getQuantity());
                    itemResponse.setItemTotal(
                            item.getProduct().getPrice()
                                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                    );
                    if (item.getProduct().getSize() != null) {
                        itemResponse.setSizeLabel(item.getProduct().getSize().getLabel());
                    }
                    return itemResponse;
                })
                .toList();

        CartResponse response = new CartResponse();
        response.setCartId(cart.getId());
        response.setUserId(cart.getUser().getId());
        response.setItems(itemResponses);
        response.setTotalItems(cart.getItems().size());
        response.setCartTotal(cart.getCartTotal());
        return response;
    }
}