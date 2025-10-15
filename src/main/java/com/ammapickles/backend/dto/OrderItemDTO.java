package com.ammapickles.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
	
    private Long id;
    private Long productId;
    
    private String productName;
    
    private int quantity;
    private BigDecimal price;
}
