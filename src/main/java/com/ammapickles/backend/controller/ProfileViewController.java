package com.ammapickles.backend.controller;

import com.ammapickles.backend.dto.user.UpdateUserRequest;
import com.ammapickles.backend.dto.address.AddressResponse;
import com.ammapickles.backend.repository.OrderRepository;
import com.ammapickles.backend.security.CustomUserDetails;
import com.ammapickles.backend.service.AddressService;
import com.ammapickles.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @GetMapping("/profile")
    public String profilePage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Long userId = userDetails.getId();
        List<AddressResponse> addresses = addressService.getAddressesByUser(userId);
        model.addAttribute("user", userService.getUserById(userId));
        model.addAttribute("addresses", addresses);
        model.addAttribute("orderCount", orderRepository.countByUserId(userId));
        model.addAttribute("addressCount", addresses.size());
        model.addAttribute("username", userDetails.getUser().getUsername());
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetails userDetails,
                                @RequestParam String fullName,
                                @RequestParam(required = false) String phone,
                                RedirectAttributes flash) {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername(fullName);
        request.setPhoneNumber(phone);
        userService.updateUser(userDetails.getId(), request);
        flash.addFlashAttribute("successMsg", "Profile updated successfully!");
        return "redirect:/profile";
    }
}
