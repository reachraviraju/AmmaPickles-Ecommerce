package com.ammapickles.backend.controller;

import com.ammapickles.backend.dto.OrderDTO;
import com.ammapickles.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    //user

    // Get all orders for a specific user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderDTO>> getOrdersByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUser(userId));
    }

    // Get specific order for a user (ownership check)
    @GetMapping("/user/{userId}/{orderId}")
    public ResponseEntity<OrderDTO> getOrderByIdForUser(
            @PathVariable Long userId,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderByIdForUser(orderId, userId));
    }

    // Place new order
    @PostMapping("/user/{userId}")
    public ResponseEntity<OrderDTO> placeOrder(
            @PathVariable Long userId,
            @RequestBody OrderDTO orderDTO) {
        OrderDTO placed = orderService.placeOrder(userId, orderDTO);
        return new ResponseEntity<>(placed, HttpStatus.CREATED);
    }

    // Cancel order
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    // admin

    // Get all orders (admin use)
    @GetMapping
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    // Get specific order by ID (admin use)
    
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }
}
