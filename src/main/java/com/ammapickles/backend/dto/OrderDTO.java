package com.ammapickles.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    private Long id;
    private Long userId;
    private List<OrderItemDTO> orderItems;
    private double totalAmount;
    private double deliveryCharge;
    private String status;
    private LocalDateTime orderDate;
    private AddressDTO deliveryAddress;
}
