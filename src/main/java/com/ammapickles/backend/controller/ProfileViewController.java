package com.ammapickles.backend.controller;

import com.ammapickles.backend.dto.user.UpdateUserRequest;
import com.ammapickles.backend.dto.user.UserResponse;
import com.ammapickles.backend.dto.address.AddressResponse;
import com.ammapickles.backend.entity.User;
import com.ammapickles.backend.repository.CustomOrderRequestRepository;
import com.ammapickles.backend.repository.OrderRepository;
import com.ammapickles.backend.repository.UserRepository;
import com.ammapickles.backend.security.CustomUserDetails;
import com.ammapickles.backend.service.AddressService;
import com.ammapickles.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProfileViewController {

    private final UserService userService;
    private final AddressService addressService;
    private final OrderRepository orderRepository;
    private final CustomOrderRequestRepository customOrderRequestRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/profile")
    public String profilePage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Long userId = userDetails.getId();
        UserResponse user = userService.getUserById(userId);
        List<AddressResponse> addresses = addressService.getAddressesByUser(userId);

        int customOrdersCount = 0;
        try {
            customOrdersCount = customOrderRequestRepository.findByUserIdOrPhone(userId, user.getPhoneNumber()).size();
        } catch (Exception ignored) {}

        model.addAttribute("user", user);
        model.addAttribute("addresses", addresses);
        model.addAttribute("orderCount", orderRepository.countByUserId(userId));
        model.addAttribute("customOrderCount", customOrdersCount);
        model.addAttribute("addressCount", addresses.size());
        model.addAttribute("username", user.getUsername());
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetails userDetails,
                                @RequestParam String fullName,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String newPassword,
                                @RequestParam(required = false) String confirmPassword,
                                RedirectAttributes flash) {
        Long userId = userDetails.getId();

        // 1. Password change if requested
        if (newPassword != null && !newPassword.isBlank()) {
            if (!newPassword.equals(confirmPassword)) {
                flash.addFlashAttribute("errorMsg", "Passwords do not match! Please check and re-enter.");
                return "redirect:/profile";
            }
            if (newPassword.length() < 6) {
                flash.addFlashAttribute("errorMsg", "New password must be at least 6 characters long.");
                return "redirect:/profile";
            }
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);
            }
        }

        // 2. Profile username & phone update
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername(fullName);
        request.setPhoneNumber(phone);
        userService.updateUser(userId, request);

        flash.addFlashAttribute("successMsg", "Profile updated successfully!");
        return "redirect:/profile";
    }
}
