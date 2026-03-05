package com.ammapickles.backend.controller;

import com.ammapickles.backend.dto.product.ProductGroupResponse;
import com.ammapickles.backend.entity.User;
import com.ammapickles.backend.exception.ResourceNotFoundException;
import com.ammapickles.backend.repository.UserRepository;
import com.ammapickles.backend.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProductViewController {

    private final ProductService productService;
    private final UserRepository userRepository;

    @GetMapping("/products/{id}")
    public String productDetail(@PathVariable Long id,
                                Model model,
                                @AuthenticationPrincipal UserDetails userDetails) {

        List<ProductGroupResponse> allProducts = productService.getAllProductsGrouped();

        ProductGroupResponse product = allProducts.stream()
                .filter(p -> p.getVariants() != null &&
                        p.getVariants().stream()
                         .anyMatch(v -> v.getId().equals(id)))
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found: " + id));

        // find selected variant
        ProductGroupResponse.ProductVariant selected = product.getVariants().stream()
                .filter(v -> v.getId().equals(id))
                .findFirst()
                .orElse(product.getVariants().get(0));

        model.addAttribute("product", product);
        model.addAttribute("selectedVariant", selected);

        if (userDetails != null) {
            User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
            model.addAttribute("username", user.getUsername());
        }

        return "product-detail";
    }
}