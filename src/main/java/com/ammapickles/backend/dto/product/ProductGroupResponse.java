package com.ammapickles.backend.dto.product;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

import com.ammapickles.backend.entity.Size;

@Data
public class ProductGroupResponse {

    private String name;
    private String description;
    private String categoryName;
    private Long categoryId;           
    private List<ProductVariant> variants;

    // Convenience getters for Thymeleaf
    public Long getId() {
        return variants != null && !variants.isEmpty()
                ? variants.get(0).getId() : null;
    }

    public BigDecimal getPrice() {
        return variants != null && !variants.isEmpty()
                ? variants.get(0).getPrice() : null;
    }

    public String getSizeLabel() {
        return variants != null && !variants.isEmpty()
                ? variants.get(0).getSizeLabel() : null;
    }

    public boolean isInStock() {
        return variants != null && variants.stream()
                .anyMatch(ProductVariant::isInStock);
    }

    @Data
    public static class ProductVariant {
        private Long id;
        private Size size;
        private String sizeLabel;
        private BigDecimal price;
        private boolean inStock;
        private int quantity;
    }
}