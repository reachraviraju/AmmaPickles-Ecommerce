package com.ammapickles.backend.controller;

import com.ammapickles.backend.dto.address.AddressRequest;
import com.ammapickles.backend.security.CustomUserDetails;
import com.ammapickles.backend.service.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AddressViewController {

    private final AddressService addressService;

    @GetMapping("/addresses/add")
    public String addAddressPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("username", userDetails.getUser().getUsername());
        return "add-address";
    }

    @PostMapping("/addresses/add")
    public String saveAddress(@RequestParam String name,
                              @RequestParam String street,
                              @RequestParam String city,
                              @RequestParam(required = false) String district,
                              @RequestParam String state,
                              @RequestParam String pincode,
                              @RequestParam String mobileNumber,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              RedirectAttributes flash) {
        try {
            AddressRequest request = new AddressRequest();
            request.setName(name);
            request.setStreet(street);
            request.setCity(city);
            request.setDistrict(district);
            request.setState(state);
            request.setPincode(pincode);
            request.setMobileNumber(mobileNumber);
            addressService.createAddress(userDetails.getId(), request);
            flash.addFlashAttribute("successMsg", "Address saved successfully! 📍");
        } catch (Exception e) {
            log.error("Failed to save address for user {}: {}", userDetails.getId(), e.getMessage(), e);
            flash.addFlashAttribute("errorMsg", e.getMessage() != null ? e.getMessage() : "Failed to save address.");
        }
        return "redirect:/orders/place";
    }

    @PostMapping("/addresses/edit/{id}")
    public String updateAddress(@PathVariable Long id,
                                @RequestParam String name,
                                @RequestParam String street,
                                @RequestParam String city,
                                @RequestParam(required = false) String district,
                                @RequestParam String state,
                                @RequestParam String pincode,
                                @RequestParam String mobileNumber,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                RedirectAttributes flash) {
        try {
            AddressRequest request = new AddressRequest();
            request.setName(name);
            request.setStreet(street);
            request.setCity(city);
            request.setDistrict(district);
            request.setState(state);
            request.setPincode(pincode);
            request.setMobileNumber(mobileNumber);
            addressService.updateAddress(id, request, userDetails.getId());
            flash.addFlashAttribute("successMsg", "Address updated successfully!");
        } catch (Exception e) {
            log.error("Failed to update address {} for user {}: {}", id, userDetails.getId(), e.getMessage(), e);
            flash.addFlashAttribute("errorMsg", e.getMessage() != null ? e.getMessage() : "Failed to update address.");
        }
        return "redirect:/orders/place";
    }

    @PostMapping("/addresses/delete/{id}")
    public String deleteAddress(@PathVariable Long id,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                RedirectAttributes flash) {
        try {
            addressService.deleteAddress(userDetails.getId(), id);
            flash.addFlashAttribute("successMsg", "Address deleted successfully!");
        } catch (Exception e) {
            log.error("Failed to delete address {} for user {}: {}", id, userDetails.getId(), e.getMessage(), e);
            flash.addFlashAttribute("errorMsg", e.getMessage() != null ? e.getMessage() : "Failed to delete address.");
        }
        return "redirect:/orders/place";
    }
}