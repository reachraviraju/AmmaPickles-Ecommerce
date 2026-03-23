package com.ammapickles.backend.service.impl;

import com.ammapickles.backend.dto.order.OrderItemResponse;
import com.ammapickles.backend.dto.order.OrderRequest;
import com.ammapickles.backend.dto.order.OrderResponse;
import com.ammapickles.backend.entity.*;
import com.ammapickles.backend.exception.ResourceNotFoundException;
import com.ammapickles.backend.repository.*;
import com.ammapickles.backend.service.EmailService;
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
    private final ProductRepository productRepository;
    private final EmailService emailService;

    private static final BigDecimal DELIVERY_CHARGE = BigDecimal.valueOf(70);
    private static final BigDecimal FREE_DELIVERY_ABOVE = BigDecimal.valueOf(1000);
    private static final BigDecimal FIRST_ORDER_FREE_ABOVE = BigDecimal.valueOf(500);

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
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        return mapToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse placeOrder(Long userId, OrderRequest request) {

        log.info("Placing order for user: {}", userId);

        // Fetch user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Fetch address
        Address address = addressRepository.findByIdAndUserId(request.getAddressId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        //  Fetch cart
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty!");
        }

        //  Calculate totals
        BigDecimal totalAmount = cart.getCartTotal();
        BigDecimal deliveryCharge = calculateDeliveryCharge(totalAmount, userId);

        // Create order
        Order order = Order.builder()
                .user(user)
                .deliveryAddress(address)
                .totalAmount(totalAmount)
                .deliveryCharge(deliveryCharge)
                .status(OrderStatus.CONFIRMED)
                .build();

        //  Convert cart -> order items
        List<OrderItem> orderItems = cart.getItems().stream()
                .map(cartItem -> {
                    Product product = cartItem.getProduct();

                    int newQty = product.getQuantity() - cartItem.getQuantity();
                    if (newQty < 0) {
                        throw new IllegalStateException("Insufficient stock for: " + product.getName());
                    }

                    product.setQuantity(newQty);
                    productRepository.save(product);

                    return OrderItem.builder()
                            .order(order)
                            .product(product)
                            .quantity(cartItem.getQuantity())
                            .price(product.getPrice())
                            .build();
                })
                .toList();

        order.setOrderItems(orderItems);

        //  Save order
        Order saved = orderRepository.save(order);

        // Send email (non-blocking logic style)
        emailService.sendOrderConfirmationEmail(
                user.getEmail(),
                user.getUsername(),
                saved.getId(),
                saved.getGrandTotal()
        );

        // Clear cart
        cart.getItems().clear();
        cartRepository.save(cart);

        log.info("Order placed successfully: {}", saved.getId());

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.CONFIRMED &&
                order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot cancel order");
        }

        //  Restore stock
        order.getOrderItems().forEach(item -> {
            Product product = item.getProduct();
            product.setQuantity(product.getQuantity() + item.getQuantity());
            productRepository.save(product);
        });

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, String status) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.setStatus(OrderStatus.valueOf(status.toUpperCase()));
        orderRepository.save(order);

        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable)
                .map(this::mapToResponse);
    }

    //  SINGLE SOURCE OF TRUTH
    private BigDecimal calculateDeliveryCharge(BigDecimal orderTotal, Long userId) {

        // Free delivery for all above ₹1000
        if (orderTotal.compareTo(FREE_DELIVERY_ABOVE) >= 0) {
            return BigDecimal.ZERO;
        }

        // First order free above ₹500
        boolean isFirstOrder = orderRepository.countByUserId(userId) == 0;

        if (isFirstOrder && orderTotal.compareTo(FIRST_ORDER_FREE_ABOVE) >= 0) {
            return BigDecimal.ZERO;
        }

        return DELIVERY_CHARGE;
    }

    private OrderResponse mapToResponse(Order order) {

        OrderResponse response = new OrderResponse();

        response.setId(order.getId());
        response.setStatus(order.getStatus());
        response.setTotalAmount(order.getTotalAmount());
        response.setDeliveryCharge(order.getDeliveryCharge());
        response.setGrandTotal(order.getGrandTotal());
        response.setOrderDate(order.getOrderDate());

        Address addr = order.getDeliveryAddress();
        if (addr != null) {
            response.setDeliveryAddress(
                   addr.getName()+", " + addr.getStreet() + ", " + addr.getCity() + ", " +
                            addr.getDistrict() + " - " + addr.getPincode()
            );
        }

        List<OrderItemResponse> items = order.getOrderItems().stream()
                .map(item -> {
                    OrderItemResponse r = new OrderItemResponse();
                    r.setProductId(item.getProduct().getId());
                    r.setProductName(item.getProduct().getName());
                    r.setQuantity(item.getQuantity());
                    r.setPriceAtTimeOfOrder(item.getPrice());
                    r.setItemTotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                    return r;
                })
                .toList();

        response.setItems(items);

        return response;
    }
}