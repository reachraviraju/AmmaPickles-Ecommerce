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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class CartViewController {

    private final CartService cartService;
    private final UserRepository userRepository;

    @GetMapping("/cart")
    public String cartPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        model.addAttribute("cart", cartService.getUserCart(user.getId()));
        model.addAttribute("username", user.getUsername());
        return "cart";
    }

    @PostMapping("/cart/add/{productId}")
    public String addToCart(@PathVariable Long productId,
                            @RequestParam(defaultValue = "1") int quantity,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes flash) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        cartService.addToCart(user.getId(), productId, quantity);
        flash.addFlashAttribute("successMsg", "Added to cart! 🛒");
        return "redirect:/home";
    }

    @PostMapping("/cart/remove/{cartItemId}")
    public String removeItem(@PathVariable Long cartItemId,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes flash) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        cartService.removeCartItem(cartItemId, user.getId());
        flash.addFlashAttribute("successMsg", "Item removed from cart!");
        return "redirect:/cart";
    }

    @PostMapping("/cart/update/{cartItemId}")
    public String updateItem(@PathVariable Long cartItemId,
                             @RequestParam int quantity,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes flash) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        if (quantity <= 0) {
            cartService.removeCartItem(cartItemId, user.getId());
            flash.addFlashAttribute("successMsg", "Item removed from cart!");
        } else {
            cartService.updateCartItem(cartItemId, quantity, user.getId());
        }
        return "redirect:/cart";
    }
}