package com.ammapickles.backend.controller;

import com.ammapickles.backend.dto.product.ProductGroupResponse;
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
                           @RequestParam(required = false) Long category,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "8") int size) {

        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("activeCategory", category);
        model.addAttribute("activeSearch", search);

        // Get all products based on filter
        List<ProductGroupResponse> allProducts;
        if (search != null && !search.isBlank()) {
            allProducts = productService.searchProductsGrouped(search);
        } else if (category != null) {
            allProducts = productService.getProductsGroupedByCategory(category);
        } else {
            allProducts = productService.getAllProductsGrouped();
        }

        // Pagination logic
        int totalProducts = allProducts.size();
        int totalPages = (int) Math.ceil((double) totalProducts / size);
        if (totalPages == 0) totalPages = 1;
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        int start = page * size;
        int end = Math.min(start + size, totalProducts);

        model.addAttribute("products", allProducts.subList(start, end));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalProducts", totalProducts);

        if (userDetails != null) {
            User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
            model.addAttribute("username", user.getUsername());
        }

        return "home";
    }
}