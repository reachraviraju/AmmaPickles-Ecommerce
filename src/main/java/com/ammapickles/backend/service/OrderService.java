package com.ammapickles.backend.service;

import com.ammapickles.backend.dto.OrderDTO;
import com.ammapickles.backend.entity.Order;
import java.util.List;

public interface OrderService {
	
	List<OrderDTO> getOrdersByUser(Long userId);
    OrderDTO placeOrder(Long userId, OrderDTO orderDTO);
    OrderDTO getOrderById(Long orderId);
    void cancelOrder(Long orderId);
}
