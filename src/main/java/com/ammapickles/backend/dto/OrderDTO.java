package com.ammapickles.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
	
	 private Long id;
	    private Long userId; 
	    private List<OrderItemDTO> orderItems;
	    private BigDecimal totalAmount;
	    
	    
	    private BigDecimal deliveryCharge;
	    
	    private String status;
	    private LocalDateTime orderDate;
	    private AddressDTO deliveryAddress;
}
