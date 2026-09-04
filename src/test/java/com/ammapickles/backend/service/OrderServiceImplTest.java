package com.ammapickles.backend.service;

import com.ammapickles.backend.dto.order.OrderRequest;
import com.ammapickles.backend.dto.order.OrderResponse;
import com.ammapickles.backend.entity.*;
import com.ammapickles.backend.exception.ResourceNotFoundException;
import com.ammapickles.backend.repository.*;
import com.ammapickles.backend.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User testAdminUser;
    private Address testAddress;
    private Product testProduct;
    private Cart testCart;
    private CartItem testCartItem;

    @BeforeEach
    void setUp() {
        Role adminRole = new Role(1L, "ROLE_ADMIN");
        testAdminUser = User.builder()
                .id(1L)
                .username("admin")
                .email("admin@ammapickles.com")
                .password("encoded_pass")
                .roles(Set.of(adminRole))
                .build();

        testAddress = Address.builder()
                .id(10L)
                .name("Ravi Raju")
                .street("MG Road")
                .city("Vijayawada")
                .district("Krishna")
                .state("Andhra Pradesh")
                .pincode("520001")
                .mobileNumber("9876543210")
                .user(testAdminUser)
                .build();

        testProduct = Product.builder()
                .id(100L)
                .name("Avakaya Mango Pickle")
                .price(BigDecimal.valueOf(350))
                .size(Size.MEDIUM)
                .quantity(20)
                .build();

        testCart = Cart.builder()
                .id(50L)
                .user(testAdminUser)
                .items(new ArrayList<>())
                .build();

        testCartItem = CartItem.builder()
                .id(501L)
                .cart(testCart)
                .product(testProduct)
                .quantity(2)
                .build();

        testCart.getItems().add(testCartItem);
    }

    @Test
    @DisplayName("Admin places order successfully - stock deducted, cart cleared, email triggered")
    void testPlaceOrder_Success() {
        OrderRequest request = new OrderRequest();
        request.setAddressId(testAddress.getId());

        when(userRepository.findById(1L)).thenReturn(Optional.of(testAdminUser));
        when(addressRepository.findByIdAndUserId(testAddress.getId(), 1L)).thenReturn(Optional.of(testAddress));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(orderRepository.countByUserId(1L)).thenReturn(0L); // first order

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(999L);
            o.setOrderDate(LocalDateTime.now());
            return o;
        });

        OrderResponse response = orderService.placeOrder(1L, request);

        assertNotNull(response);
        assertEquals(999L, response.getId());
        assertEquals(OrderStatus.CONFIRMED, response.getStatus());
        assertEquals(BigDecimal.valueOf(700), response.getTotalAmount()); // 350 * 2
        // First order above 500 has free delivery
        assertEquals(BigDecimal.ZERO, response.getDeliveryCharge());
        assertEquals(BigDecimal.valueOf(700), response.getGrandTotal());

        // Stock deduction check: 20 - 2 = 18
        assertEquals(18, testProduct.getQuantity());
        verify(productRepository, times(1)).save(testProduct);

        // Cart cleared
        assertTrue(testCart.getItems().isEmpty());
        verify(cartRepository, times(1)).save(testCart);

        // Email triggered
        verify(emailService, times(1)).sendOrderConfirmationEmail(
                eq(testAdminUser.getEmail()), eq(testAdminUser.getUsername()), eq(999L), any());

        // Items in response have sizeLabel
        assertEquals(1, response.getItems().size());
        assertEquals("1 kg", response.getItems().get(0).getSizeLabel());
    }

    @Test
    @DisplayName("Place order fails if stock is insufficient")
    void testPlaceOrder_InsufficientStock() {
        testCartItem.setQuantity(50); // available is 20
        OrderRequest request = new OrderRequest();
        request.setAddressId(testAddress.getId());

        when(userRepository.findById(1L)).thenReturn(Optional.of(testAdminUser));
        when(addressRepository.findByIdAndUserId(testAddress.getId(), 1L)).thenReturn(Optional.of(testAddress));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.placeOrder(1L, request));
        assertTrue(ex.getMessage().contains("Insufficient stock"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Place order fails if cart is empty")
    void testPlaceOrder_EmptyCart() {
        testCart.getItems().clear();
        OrderRequest request = new OrderRequest();
        request.setAddressId(testAddress.getId());

        when(userRepository.findById(1L)).thenReturn(Optional.of(testAdminUser));
        when(addressRepository.findByIdAndUserId(testAddress.getId(), 1L)).thenReturn(Optional.of(testAddress));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.placeOrder(1L, request));
        assertEquals("Cart is empty!", ex.getMessage());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Place order succeeds even if email dispatch encounters an exception")
    void testPlaceOrder_EmailFailure_DoesNotRollbackOrder() {
        OrderRequest request = new OrderRequest();
        request.setAddressId(testAddress.getId());

        when(userRepository.findById(1L)).thenReturn(Optional.of(testAdminUser));
        when(addressRepository.findByIdAndUserId(testAddress.getId(), 1L)).thenReturn(Optional.of(testAddress));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(orderRepository.countByUserId(1L)).thenReturn(1L);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(1001L);
            o.setOrderDate(LocalDateTime.now());
            return o;
        });

        doThrow(new RuntimeException("Brevo API timeout")).when(emailService)
                .sendOrderConfirmationEmail(any(), any(), any(), any());

        OrderResponse response = assertDoesNotThrow(() -> orderService.placeOrder(1L, request));
        assertNotNull(response);
        assertEquals(1001L, response.getId());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Cancel confirmed order restores product stock")
    void testCancelOrder_RestoresStock() {
        Order order = Order.builder()
                .id(200L)
                .user(testAdminUser)
                .status(OrderStatus.CONFIRMED)
                .orderItems(new ArrayList<>())
                .build();

        OrderItem item = OrderItem.builder()
                .id(300L)
                .order(order)
                .product(testProduct)
                .quantity(3)
                .price(testProduct.getPrice())
                .build();
        order.getOrderItems().add(item);

        when(orderRepository.findByIdAndUserId(200L, 1L)).thenReturn(Optional.of(order));

        int initialQty = testProduct.getQuantity();
        orderService.cancelOrder(200L, 1L);

        assertEquals(initialQty + 3, testProduct.getQuantity());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(productRepository, times(1)).save(testProduct);
        verify(orderRepository, times(1)).save(order);
    }
}
