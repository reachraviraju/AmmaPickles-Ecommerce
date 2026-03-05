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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AddressViewController {

    private final AddressService addressService;
    private final UserRepository userRepository;

    // VIEW ALL ADDRESSES
    @GetMapping("/addresses")
    public String addressesPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        model.addAttribute("addresses", addressService.getAddressesByUser(user.getId()));
        model.addAttribute("username", user.getUsername());
        return "addresses";
    }

    // SHOW ADD ADDRESS FORM
    @GetMapping("/addresses/add")
    public String addAddressPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        model.addAttribute("username", user.getUsername());
        return "add-address";
    }

    // SUBMIT NEW ADDRESS
    @PostMapping("/addresses/add")
    public String saveAddress(@RequestParam String street,
                              @RequestParam String city,
                              @RequestParam(required = false) String district,
                              @RequestParam String state,
                              @RequestParam String pincode,
                              @RequestParam Double distanceInKm,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

        AddressRequest request = new AddressRequest();
        request.setStreet(street);
        request.setCity(city);
        request.setDistrict(district);
        request.setState(state);
        request.setPincode(pincode);
        request.setDistanceInKm(distanceInKm);

        addressService.createAddress(user.getId(), request);
        return "redirect:/orders/place";
    }
}