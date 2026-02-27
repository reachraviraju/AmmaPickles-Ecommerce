package com.ammapickles.backend.dto.product;

import com.ammapickles.backend.entity.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Size size;
    private String sizeLabel;       // "1/2 kg", "1 kg", "2 kg" 
    private Integer quantity;
    private boolean inStock;        
    private Long categoryId;
    private String categoryName;   
}
