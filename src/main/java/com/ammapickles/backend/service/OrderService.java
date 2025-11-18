package com.ammapickles.backend.service;

import com.ammapickles.backend.dto.OrderDTO;
import java.util.List;

public interface OrderService {
	
	//user 
	
	 // Get all orders
    List<OrderDTO> getOrdersByUser(Long userId);

    // Get specific order ensuring it belongs to that user
    OrderDTO getOrderByIdForUser(Long orderId, Long userId);

    // Place a new order
    OrderDTO placeOrder(Long userId, OrderDTO orderDTO);

    // Cancel an existing order
    void cancelOrder(Long orderId);

    
    
    // admin

    // Get all orders in the system
    List<OrderDTO> getAllOrders();

    // Get a specific order by ID (admin)
    OrderDTO getOrderById(Long orderId);

    

   
}
