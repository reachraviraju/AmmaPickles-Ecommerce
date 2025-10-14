package com.ammapickles.backend.service.impl;

import com.ammapickles.backend.dto.CartItemDTO;
import com.ammapickles.backend.entity.Cart;
import com.ammapickles.backend.entity.CartItem;
import com.ammapickles.backend.entity.Product;
import com.ammapickles.backend.entity.User;
import com.ammapickles.backend.repository.CartItemRepository;
import com.ammapickles.backend.repository.CartRepository;
import com.ammapickles.backend.repository.ProductRepository;
import com.ammapickles.backend.repository.UserRepository;
import com.ammapickles.backend.service.CartService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public List<CartItemDTO> getCartItems(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });

        return cart.getItems()
                .stream()
                .map(item -> modelMapper.map(item, CartItemDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public CartItemDTO addToCart(Long userId, Long productId, int quantity) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });

        // Check if product already in cart
        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            cartItemRepository.save(existingItem);
            return modelMapper.map(existingItem, CartItemDTO.class);
        }

        CartItem newItem = new CartItem();
        newItem.setCart(cart);
        newItem.setProduct(product);
        newItem.setQuantity(quantity);

        cart.getItems().add(newItem);
        cartRepository.save(cart); //  saves cart items

        return modelMapper.map(newItem, CartItemDTO.class);
    }
    
    
    @Override
    public CartItemDTO updateCartItem(Long cartItemId, CartItemDTO cartItemDTO) {
        CartItem existingItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        existingItem.setQuantity(cartItemDTO.getQuantity());

        CartItem updatedItem = cartItemRepository.save(existingItem);
        return modelMapper.map(updatedItem, CartItemDTO.class);
    }

    @Override
    public void removeCartItem(Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));
        cartItemRepository.delete(item);
    }

    @Override
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        cart.getItems().clear();
        cartRepository.save(cart);
    }
}
