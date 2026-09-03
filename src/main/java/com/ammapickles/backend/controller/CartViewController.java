package com.ammapickles.backend.controller;

import com.ammapickles.backend.security.CustomUserDetails;
import com.ammapickles.backend.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @GetMapping("/cart")
    public String cartPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("cart", cartService.getUserCart(userDetails.getId()));
        model.addAttribute("username", userDetails.getUser().getUsername());
        return "cart";
    }

    @PostMapping("/cart/add/{productId}")
    public String addToCart(@PathVariable Long productId,
                            @RequestParam(defaultValue = "1") int quantity,
                            @AuthenticationPrincipal CustomUserDetails userDetails,
                            RedirectAttributes flash) {
        cartService.addToCart(userDetails.getId(), productId, quantity);
        flash.addFlashAttribute("successMsg", "Added to cart! 🛒");
        return "redirect:/home";
    }

    @PostMapping("/cart/remove/{cartItemId}")
    public String removeItem(@PathVariable Long cartItemId,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             RedirectAttributes flash) {
        cartService.removeCartItem(cartItemId, userDetails.getId());
        flash.addFlashAttribute("successMsg", "Item removed from cart!");
        return "redirect:/cart";
    }

    @PostMapping("/cart/update/{cartItemId}")
    public String updateItem(@PathVariable Long cartItemId,
                             @RequestParam int quantity,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             RedirectAttributes flash) {
        if (quantity <= 0) {
            cartService.removeCartItem(cartItemId, userDetails.getId());
            flash.addFlashAttribute("successMsg", "Item removed from cart!");
        } else {
            cartService.updateCartItem(cartItemId, quantity, userDetails.getId());
        }
        return "redirect:/cart";
    }
}