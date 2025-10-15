package com.ammapickles.backend.dto;

import com.ammapickles.backend.entity.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
	
	
    private Long id;
    private String name;
    
    private String description;
    private BigDecimal price;
    
    private Size size;     
    
    
    private Integer quantity;
    
    private String categoryName; 
    private Long categoryId;
}
