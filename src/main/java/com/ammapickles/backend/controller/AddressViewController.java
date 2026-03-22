package com.ammapickles.backend.controller;

import com.ammapickles.backend.dto.address.AddressRequest;
import com.ammapickles.backend.entity.User;
import com.ammapickles.backend.repository.UserRepository;
import com.ammapickles.backend.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class AddressViewController {

    private final AddressService addressService;
    private final UserRepository userRepository;

    @GetMapping("/addresses/add")
    public String addAddressPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        model.addAttribute("username", user.getUsername());
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
                              @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        AddressRequest request = new AddressRequest();
        request.setName(name);
        request.setStreet(street);
        request.setCity(city);
        request.setDistrict(district);
        request.setState(state);
        request.setPincode(pincode);
        request.setMobileNumber(mobileNumber);
        addressService.createAddress(user.getId(), request);
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
                                @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        AddressRequest request = new AddressRequest();
        request.setName(name);
        request.setStreet(street);
        request.setCity(city);
        request.setDistrict(district);
        request.setState(state);
        request.setPincode(pincode);
        request.setMobileNumber(mobileNumber);
        addressService.updateAddress(id, request, user.getId());
        return "redirect:/orders/place";
    }

    @PostMapping("/addresses/delete/{id}")
    public String deleteAddress(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        addressService.deleteAddress(user.getId(), id);
        return "redirect:/orders/place";
    }
}