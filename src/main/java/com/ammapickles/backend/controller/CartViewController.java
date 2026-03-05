package com.ammapickles.backend.controller;

import com.ammapickles.backend.entity.User;
import com.ammapickles.backend.repository.UserRepository;
import com.ammapickles.backend.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class CartViewController {

    private final CartService cartService;
    private final UserRepository userRepository;

   
    @GetMapping("/cart")
    public String cartPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        model.addAttribute("cart", cartService.getUserCart(user.getId()));
        model.addAttribute("username", user.getUsername()); // show name not email
        return "cart";
    }

    
    @PostMapping("/cart/add/{productId}")
    public String addToCart(@PathVariable Long productId,
                            @RequestParam(defaultValue = "1") int quantity,
                            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        cartService.addToCart(user.getId(), productId, quantity);
        return "redirect:/cart";
    }

    
    @PostMapping("/cart/remove/{cartItemId}")
    public String removeItem(@PathVariable Long cartItemId) {
        cartService.removeCartItem(cartItemId);
        return "redirect:/cart";
    }

  
    @PostMapping("/cart/update/{cartItemId}")
    public String updateItem(@PathVariable Long cartItemId,
                             @RequestParam int quantity) {
        if (quantity <= 0) {
            cartService.removeCartItem(cartItemId);
        } else {
            cartService.updateCartItem(cartItemId, quantity);
        }
        return "redirect:/cart";
    }
}