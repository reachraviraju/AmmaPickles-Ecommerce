package com.ammapickles.backend.service.impl;

import com.ammapickles.backend.dto.order.OrderItemResponse;
import com.ammapickles.backend.dto.order.OrderRequest;
import com.ammapickles.backend.dto.order.OrderResponse;
import com.ammapickles.backend.entity.*;
import com.ammapickles.backend.exception.ResourceNotFoundException;
import com.ammapickles.backend.repository.*;
import com.ammapickles.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;

    // Delivery charge constants — easy to change later
    private static final BigDecimal CHARGE_PER_KM = BigDecimal.valueOf(5);
    private static final BigDecimal MIN_DELIVERY_CHARGE = BigDecimal.valueOf(30);
    private static final BigDecimal FREE_DELIVERY_ABOVE = BigDecimal.valueOf(500);

    // CUSTOMER OPERATIONS

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(Long userId) {
        log.info("Fetching orders for user: {}", userId);

        return orderRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByIdForUser(Long orderId, Long userId) {
        log.info("Fetching order {} for user {}", orderId, userId);

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + orderId));

        return mapToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse placeOrder(Long userId, OrderRequest request) {
        log.info("Placing order for user: {}", userId);

        // Step 1: Load user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Step 2: Load delivery address —> verify it belongs to this user
        Address address = addressRepository.findByIdAndUserId(request.getAddressId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found or doesn't belong to user"));

        // Step 3: Load cart
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));

        // Step 4: Validate cart is not empty
        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot place order — cart is empty!");
        }

        // Step 5: Calculate totals
        BigDecimal totalAmount = cart.getCartTotal();
        BigDecimal deliveryCharge = calculateDeliveryCharge(address.getDistanceInKm(), totalAmount);

        // Step 6: Build order
        Order order = Order.builder()
                .user(user)
                .deliveryAddress(address)
                .totalAmount(totalAmount)
                .deliveryCharge(deliveryCharge)
                .status(OrderStatus.PENDING)
                .build();

        // Step 7: Convert cart items -> order items
        List<OrderItem> orderItems = cart.getItems().stream()
                .map(cartItem -> OrderItem.builder()
                        .order(order)
                        .product(cartItem.getProduct())
                        .quantity(cartItem.getQuantity())
                        .price(cartItem.getProduct().getPrice()) // capture price at time of order
                        .build())
                .toList();

        order.setOrderItems(orderItems);

        // Step 8: Save order
        Order saved = orderRepository.save(order);

        // Step 9: Clear cart after successful order
        cart.getItems().clear();
        cartRepository.save(cart);

        log.info("Order placed successfully with id: {}", saved.getId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        log.info("Cancelling order {} for user {}", orderId, userId);

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        // Only PENDING orders can be cancelled
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot cancel order — current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        // Dirty checking handles save
        log.info("Order cancelled: {}", orderId);
    }

    // ADMIN OPERATIONS

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        log.info("Admin fetching all orders - page: {}", pageable.getPageNumber());
        return orderRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        log.info("Admin fetching order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        return mapToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, String status) {
        log.info("Updating order {} status to {}", orderId, status);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        order.setStatus(OrderStatus.valueOf(status.toUpperCase()));
        log.info("Order status updated: {} → {}", orderId, status);

        return mapToResponse(order);
    }

    //  DELIVERY CHARGE CALCULATION 

    private BigDecimal calculateDeliveryCharge(double distanceInKm, BigDecimal orderTotal) {
        // Free delivery for orders above ₹500
        if (orderTotal.compareTo(FREE_DELIVERY_ABOVE) >= 0) {
            return BigDecimal.ZERO;
        }

        // ₹5 per km, minimum ₹30
        BigDecimal charge = CHARGE_PER_KM.multiply(BigDecimal.valueOf(distanceInKm));
        return charge.max(MIN_DELIVERY_CHARGE);
    }

    // PRIVATE HELPER 

    private OrderResponse mapToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setStatus(order.getStatus());
        response.setTotalAmount(order.getTotalAmount());
        response.setDeliveryCharge(order.getDeliveryCharge());
        response.setGrandTotal(order.getGrandTotal());
        response.setOrderDate(order.getOrderDate());

        // Format delivery address as readable string
        Address addr = order.getDeliveryAddress();
        if (addr != null) {
            response.setDeliveryAddress(
                    addr.getStreet() + ", " + addr.getCity() + ", " +
                    addr.getDistrict() + " - " + addr.getPincode()
            );
        }

        // Map order items
        List<OrderItemResponse> items = order.getOrderItems().stream()
                .map(item -> {
                    OrderItemResponse itemResponse = new OrderItemResponse();
                    itemResponse.setProductId(item.getProduct().getId());
                    itemResponse.setProductName(item.getProduct().getName());
                    itemResponse.setQuantity(item.getQuantity());
                    itemResponse.setPriceAtTimeOfOrder(item.getPrice());
                    itemResponse.setItemTotal(
                            item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                    );
                    if (item.getProduct().getSize() != null) {
                        itemResponse.setSizeLabel(item.getProduct().getSize().getLabel());
                    }
                    return itemResponse;
                })
                .toList();

        response.setItems(items);
        return response;
    }
}