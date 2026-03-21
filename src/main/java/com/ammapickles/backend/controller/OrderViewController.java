package com.ammapickles.backend.controller;

import com.ammapickles.backend.dto.order.OrderRequest;
import com.ammapickles.backend.entity.User;
import com.ammapickles.backend.repository.UserRepository;
import com.ammapickles.backend.service.AddressService;
import com.ammapickles.backend.service.OrderService;
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
public class OrderViewController {

    private final OrderService orderService;
    private final AddressService addressService;
    private final UserRepository userRepository;

    @GetMapping("/orders")
    public String ordersPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        model.addAttribute("orders", orderService.getOrdersByUser(user.getId()));
        model.addAttribute("username", user.getUsername());
        return "orders";
    }

    @GetMapping("/orders/place")
    public String placeOrderPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        model.addAttribute("addresses", addressService.getAddressesByUser(user.getId()));
        model.addAttribute("username", user.getUsername());
        return "place-order";
    }

    @PostMapping("/orders/place")
    public String submitOrder(@RequestParam Long addressId,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes flash) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        OrderRequest request = new OrderRequest();
        request.setAddressId(addressId);
        orderService.placeOrder(user.getId(), request);
        flash.addFlashAttribute("successMsg", "Order placed successfully! 🎉");
        return "redirect:/orders";
    }

    @PostMapping("/orders/cancel/{id}")
    public String cancelOrder(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes flash) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        orderService.cancelOrder(id, user.getId());
        flash.addFlashAttribute("successMsg", "Order cancelled successfully!");
        return "redirect:/orders";
    }
}