package com.ammapickles.backend.dto;

import com.ammapickles.backend.entity.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long id;
    private String name;
    private String description;
    private double price;
    private Size size;     
   
    private Integer quantity;
    private String categoryName; 
    
 
    private Long categoryId;
}
