package com.ammapickles.backend.controller;

import com.ammapickles.backend.dto.user.UpdateUserRequest;
import com.ammapickles.backend.entity.User;
import com.ammapickles.backend.repository.OrderRepository;
import com.ammapickles.backend.repository.UserRepository;
import com.ammapickles.backend.service.AddressService;
import com.ammapickles.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ProfileViewController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final AddressService addressService;
    private final OrderRepository orderRepository;

    @GetMapping("/profile")
    public String profilePage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        model.addAttribute("user", userService.getUserById(user.getId()));
        model.addAttribute("addresses", addressService.getAddressesByUser(user.getId()));
        model.addAttribute("orderCount", orderRepository.countByUserId(user.getId()));
        model.addAttribute("addressCount", addressService.getAddressesByUser(user.getId()).size());
        model.addAttribute("username", user.getUsername());
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                @RequestParam String fullName,
                                @RequestParam(required = false) String phone,
                                RedirectAttributes flash) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername(fullName);
        request.setPhoneNumber(phone);
        userService.updateUser(user.getId(), request);
        flash.addFlashAttribute("successMsg", "Profile updated successfully!");
        return "redirect:/profile";
    }
}