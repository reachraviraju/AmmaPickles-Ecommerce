package com.ammapickles.backend.service.impl;

import com.ammapickles.backend.dto.OrderDTO;
import com.ammapickles.backend.dto.OrderItemDTO;
import com.ammapickles.backend.entity.*;
import com.ammapickles.backend.exception.ResourceNotFoundException;
import com.ammapickles.backend.repository.*;
import com.ammapickles.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final CartRepository cartRepository;
    private final ModelMapper modelMapper;

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    // user 

    @Override
    public List<OrderDTO> getOrdersByUser(Long userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public OrderDTO getOrderByIdForUser(Long orderId, Long userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found or access denied"));
        return mapToDTO(order);
    }

    @Override
    public OrderDTO placeOrder(Long userId, OrderDTO orderDTO) {
        User user = getUserOrThrow(userId);
        Address address = getAddressOrThrow(orderDTO.getDeliveryAddress().getId());
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user id: " + userId));

        if (cart.getItems().isEmpty())
            throw new IllegalStateException("Cart is empty. Cannot place order.");

        // Create order
        Order order = new Order();
        order.setUser(user);
        order.setDeliveryAddress(address);
        order.setStatus(OrderStatus.PENDING);
        order.setDeliveryCharge(ZERO);

        // Map cart items to order items
        List<OrderItem> orderItems = cart.getItems().stream()
                .map(cartItem -> {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrder(order);
                    orderItem.setProduct(cartItem.getProduct());
                    orderItem.setQuantity(cartItem.getQuantity());
                    orderItem.setPrice(
                            cartItem.getProduct().getPrice()
                                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()))
                    );
                    return orderItem;
                })
                .collect(Collectors.toList());

        // Calculate total
        BigDecimal totalAmount = orderItems.stream()
                .map(OrderItem::getPrice)
                .reduce(ZERO, BigDecimal::add);

        order.setTotalAmount(totalAmount);

        // Save order + items
        Order savedOrder = orderRepository.save(order);
        orderItemRepository.saveAll(orderItems);

        // Clear cart
        clearCart(cart);

        return mapToDTO(savedOrder);
    }

    @Override
    public void cancelOrder(Long orderId) {
        Order order = getOrderOrThrow(orderId);

        if (order.getStatus() == OrderStatus.DELIVERED)
            throw new IllegalStateException("Delivered orders cannot be cancelled");

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    // admin 

    @Override
    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public OrderDTO getOrderById(Long orderId) {
        return mapToDTO(getOrderOrThrow(orderId));
    }

    // helper methods 

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private Address getAddressOrThrow(Long addressId) {
        return addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));
    }

    private Order getOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
    }

    private void clearCart(Cart cart) {
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private OrderDTO mapToDTO(Order order) {
        OrderDTO dto = modelMapper.map(order, OrderDTO.class);
        dto.setUserId(order.getUser().getId());

        if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
            List<OrderItemDTO> items = order.getOrderItems().stream()
                    .map(item -> {
                        OrderItemDTO itemDTO = new OrderItemDTO();
                        itemDTO.setId(item.getId());
                        itemDTO.setProductId(item.getProduct().getId());
                        itemDTO.setProductName(item.getProduct().getName());
                        itemDTO.setQuantity(item.getQuantity());
                        itemDTO.setPrice(item.getPrice());
                        return itemDTO;
                    })
                    .collect(Collectors.toList());
            dto.setOrderItems(items);
        }

        return dto;
    }
}
