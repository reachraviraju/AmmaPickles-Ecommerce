package com.ammapickles.backend.service;

import com.ammapickles.backend.dto.order.OrderRequest;
import com.ammapickles.backend.dto.order.OrderResponse;
import com.ammapickles.backend.entity.OrderStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {

    // CUSTOMER OPERATIONS 

    // Get all orders for a specific user
    List<OrderResponse> getOrdersByUser(Long userId);

    // Get specific order  -> verify it belongs to that user 
    OrderResponse getOrderByIdForUser(Long orderId, Long userId);

    // Place order —> userId comes from JWT token (not from request body)
    OrderResponse placeOrder(Long userId, OrderRequest request);

    // only PENDING orders can be cancelled
    void cancelOrder(Long orderId, Long userId);  // userId to verify ownership
    
    

    //  ADMIN OPERATIONS 

       // Paginated —> admin could have thousands of orders
    Page<OrderResponse> getAllOrders(Pageable pageable);

      // Get any order by ID (admin has access to all)
    OrderResponse getOrderById(Long orderId);

    // Admin (PENDING -> SHIPPED ->  DELIVERED)
    OrderResponse updateOrderStatus(Long orderId, String status);
    
    Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable);
}