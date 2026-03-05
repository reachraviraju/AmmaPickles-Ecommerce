package com.ammapickles.backend.controller;

import com.ammapickles.backend.dto.product.ProductGroupResponse;
import com.ammapickles.backend.dto.product.ProductResponse;
import com.ammapickles.backend.entity.User;
import com.ammapickles.backend.repository.CategoryRepository;
import com.ammapickles.backend.repository.UserRepository;
import com.ammapickles.backend.service.ProductService;
import lombok.RequiredArgsConstructor;

import java.util.List;


import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class HomeViewController {

    private final ProductService productService;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    
    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String homePage(Model model,
                           @AuthenticationPrincipal UserDetails userDetails,
                           @RequestParam(required = false) String search,
                           @RequestParam(required = false) Long category) {

        // always send categories for filter buttons
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("activeCategory", category);
        model.addAttribute("activeSearch", search);

        if (search != null && !search.isBlank()) {
        	
        	model.addAttribute("products", productService.searchProductsGrouped(search));
        	
        } else if (category != null) {
        	
        	model.addAttribute("products", productService.getProductsGroupedByCategory(category));
        	
        } else {
        	model.addAttribute("products", productService.getAllProductsGrouped());
        }

        if (userDetails != null) {
            User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
            model.addAttribute("username", user.getUsername());
        }

        return "home";
    }
    
}