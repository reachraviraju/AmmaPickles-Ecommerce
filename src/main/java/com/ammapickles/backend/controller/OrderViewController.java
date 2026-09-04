package com.ammapickles.backend.controller;

import com.ammapickles.backend.dto.order.OrderRequest;
import com.ammapickles.backend.security.CustomUserDetails;
import com.ammapickles.backend.service.AddressService;
import com.ammapickles.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
public class OrderViewController {

    private final OrderService orderService;
    private final AddressService addressService;

    @GetMapping("/orders")
    public String ordersPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("orders", orderService.getOrdersByUser(userDetails.getId()));
        model.addAttribute("username", userDetails.getUser().getUsername());
        return "orders";
    }

    @GetMapping("/orders/place")
    public String placeOrderPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("addresses", addressService.getAddressesByUser(userDetails.getId()));
        model.addAttribute("username", userDetails.getUser().getUsername());
        return "place-order";
    }

    @PostMapping("/orders/place")
    public String submitOrder(@RequestParam Long addressId,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              RedirectAttributes flash) {
        try {
            OrderRequest request = new OrderRequest();
            request.setAddressId(addressId);
            orderService.placeOrder(userDetails.getId(), request);
            flash.addFlashAttribute("successMsg", "Order placed successfully! 🎉");
            return "redirect:/orders";
        } catch (Exception e) {
            log.error("Failed to place order for user {}: {}", userDetails.getId(), e.getMessage(), e);
            flash.addFlashAttribute("errorMsg", e.getMessage() != null ? e.getMessage() : "Failed to place order. Please try again.");
            return "redirect:/orders/place";
        }
    }

    @PostMapping("/orders/cancel/{id}")
    public String cancelOrder(@PathVariable Long id,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              RedirectAttributes flash) {
        try {
            orderService.cancelOrder(id, userDetails.getId());
            flash.addFlashAttribute("successMsg", "Order cancelled successfully!");
        } catch (Exception e) {
            log.error("Failed to cancel order {} for user {}: {}", id, userDetails.getId(), e.getMessage(), e);
            flash.addFlashAttribute("errorMsg", e.getMessage() != null ? e.getMessage() : "Failed to cancel order.");
        }
        return "redirect:/orders";
    }
}