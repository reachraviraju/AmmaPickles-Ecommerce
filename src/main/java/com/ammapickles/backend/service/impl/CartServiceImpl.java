package com.ammapickles.backend.service.impl;

import com.ammapickles.backend.dto.CartItemDTO;
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
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    public List<CartItemDTO> getCartItems(Long userId) {
    	
    	Cart cart = cartRepository.findByUserId(userId)
    	        .orElseThrow(() -> new ResourceNotFoundException("Cart not found for userId: " + userId));

        return cart.getItems().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CartItemDTO addToCart(Long userId, Long productId, int quantity) {
        
    	User user = userRepository.findById(userId)
    	        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
    	Product product = productRepository.findById(productId)
    	        .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
    	
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setUser(user);
            return cartRepository.save(newCart);
        });

        CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setCart(cart);
                    newItem.setProduct(product);
                    cart.getItems().add(newItem);
                    return newItem;
                });

        cartItem.setQuantity(cartItem.getQuantity() == null ? quantity : cartItem.getQuantity() + quantity);
        CartItem saved = cartItemRepository.save(cartItem);
        return mapToDTO(saved);
    }

    @Override
    public CartItemDTO updateCartItem(Long cartItemId, CartItemDTO cartItemDTO) {
    	
    	CartItem existing = cartItemRepository.findById(cartItemId)
    	        .orElseThrow(() -> new ResourceNotFoundException("CartItem not found with id: " + cartItemId));
 
        existing.setQuantity(cartItemDTO.getQuantity());
        CartItem updated = cartItemRepository.save(existing);
        return mapToDTO(updated);
    }

    @Override
    public void removeCartItem(Long cartItemId) {
        if (!cartItemRepository.existsById(cartItemId)) {
            throw new ResourceNotFoundException("CartItem not found with id: " + cartItemId);
            
        }
        cartItemRepository.deleteById(cartItemId);
    }

    @Override
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for userId: " + userId));

        cart.getItems().clear();
        cartItemRepository.deleteAll(cart.getItems());
    }

    private CartItemDTO mapToDTO(CartItem cartItem) {
        CartItemDTO dto = new CartItemDTO();
        dto.setId(cartItem.getId());
        dto.setProductId(cartItem.getProduct().getId());
        dto.setProductName(cartItem.getProduct().getName());
        dto.setPrice(cartItem.getProduct().getPrice());
        dto.setQuantity(cartItem.getQuantity());
        return dto;
    }
}
