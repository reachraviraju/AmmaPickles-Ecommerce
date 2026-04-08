package com.ammapickles.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_name", columnList = "name"),
    @Index(name = "idx_product_category", columnList = "category_id"),
    @Index(name = "idx_product_name_category", columnList = "name, category_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
	
	

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    
    @NotBlank(message = "Product name is required")
    @Column(nullable = false)
    private String name;

    private String description;

   
    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price cannot be negative")
    @Column(nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    
    private Size size;

    
    @Min(value = 0, message = "Quantity cannot be negative")
    @Column(nullable = false)
    private Integer quantity;

    
    // This avoids writing quantity checks again and again in service layer
    public boolean isInStock() {
        return quantity != null && quantity > 0;
        
    }

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}