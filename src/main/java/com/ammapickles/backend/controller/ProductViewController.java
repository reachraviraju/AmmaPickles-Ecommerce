package com.ammapickles.backend.controller;

import com.ammapickles.backend.dto.product.ProductGroupResponse;
import com.ammapickles.backend.exception.ResourceNotFoundException;
import com.ammapickles.backend.security.CustomUserDetails;
import com.ammapickles.backend.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class ProductViewController {

    private final ProductService productService;
    
    @GetMapping("/products/{id}")
    public String productDetail(@PathVariable Long id,
                                Model model,
                                @AuthenticationPrincipal CustomUserDetails userDetails) {

        ProductGroupResponse product = productService.getProductGroupByVariantId(id);

        ProductGroupResponse.ProductVariant selected = product.getVariants().stream()
                .filter(v -> v.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found: " + id));

        model.addAttribute("product", product);
        model.addAttribute("selectedVariant", selected);

        if (userDetails != null && userDetails.getUser() != null) {
            model.addAttribute("username", userDetails.getUser().getUsername());
        }
        return "product-detail";
    }
}
