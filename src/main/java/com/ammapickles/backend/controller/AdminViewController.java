package com.ammapickles.backend.controller;

import com.ammapickles.backend.dto.order.OrderResponse;
import com.ammapickles.backend.dto.product.ProductRequest;
import com.ammapickles.backend.entity.OrderStatus;
import com.ammapickles.backend.entity.Size;
import com.ammapickles.backend.repository.OrderRepository;
import com.ammapickles.backend.repository.UserRepository;
import com.ammapickles.backend.service.CategoryService;
import com.ammapickles.backend.service.OrderService;
import com.ammapickles.backend.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminViewController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductService productService;
    private final CategoryService categoryService;

    // DASHBOARD
    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("totalOrders",     orderRepository.count());
        model.addAttribute("totalUsers",      userRepository.count());
        model.addAttribute("totalRevenue",    orderRepository.getTotalRevenue(OrderStatus.CANCELLED));
        model.addAttribute("pendingOrders",   orderRepository.countByStatus(OrderStatus.CONFIRMED));
        model.addAttribute("shippedOrders",   orderRepository.countByStatus(OrderStatus.SHIPPED));
        model.addAttribute("deliveredOrders", orderRepository.countByStatus(OrderStatus.DELIVERED));
        model.addAttribute("cancelledOrders", orderRepository.countByStatus(OrderStatus.CANCELLED));
        model.addAttribute("activePage",      "dashboard");
        return "admin/dashboard";
    }

    // ORDERS
    @GetMapping("/orders")
    public String ordersList(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             @RequestParam(required = false) String status,
                             Model model) {
        Page<OrderResponse> orders;
        if (status != null && !status.isBlank()) {
            orders = orderService.getOrdersByStatus(
                    OrderStatus.valueOf(status.toUpperCase()),
                    PageRequest.of(page, size, Sort.by("id").descending()));
        } else {
            orders = orderService.getAllOrders(
                    PageRequest.of(page, size, Sort.by("id").descending()));
        }
        model.addAttribute("orders",     orders);
        model.addAttribute("status",     status);
        model.addAttribute("statuses",   OrderStatus.values());
        model.addAttribute("activePage", "orders");
        return "admin/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        model.addAttribute("order",      orderService.getOrderById(id));
        model.addAttribute("statuses",   OrderStatus.values());
        model.addAttribute("activePage", "orders");
        return "admin/order-detail";
    }

    @PostMapping("/orders/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam String status,
                               RedirectAttributes flash) {
        orderService.updateOrderStatus(id, status);
        flash.addFlashAttribute("success", "Order #" + id + " updated to " + status);
        return "redirect:/admin/orders/" + id;
    }

    // PRODUCTS
    @GetMapping("/products")
    public String productsList(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               Model model) {
        model.addAttribute("products",   productService.getAllProducts(
                PageRequest.of(page, size, Sort.by("id").descending())));
        model.addAttribute("activePage", "products");
        return "admin/products";
    }

    @GetMapping("/products/add")
    public String addProductPage(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("sizes",      Size.values());
        model.addAttribute("activePage", "products");
        return "admin/product-form";
    }

    @PostMapping("/products/add")
    public String addProduct(@RequestParam String name,
                             @RequestParam(required = false) String description,
                             @RequestParam BigDecimal price,
                             @RequestParam Integer quantity,
                             @RequestParam Long categoryId,
                             @RequestParam(required = false) Size size,
                             RedirectAttributes flash) {
        ProductRequest request = new ProductRequest();
        request.setName(name);
        request.setDescription(description);
        request.setPrice(price);
        request.setQuantity(quantity);
        request.setCategoryId(categoryId);
        request.setSize(size);
        productService.addProduct(request);
        flash.addFlashAttribute("success", "Product added successfully!");
        return "redirect:/admin/products";
    }

    @GetMapping("/products/edit/{id}")
    public String editProductPage(@PathVariable Long id, Model model) {
        model.addAttribute("product",    productService.getProductById(id));
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("sizes",      Size.values());
        model.addAttribute("activePage", "products");
        return "admin/product-form";
    }

    @PostMapping("/products/edit/{id}")
    public String editProduct(@PathVariable Long id,
                              @RequestParam String name,
                              @RequestParam(required = false) String description,
                              @RequestParam BigDecimal price,
                              @RequestParam Integer quantity,
                              @RequestParam Long categoryId,
                              @RequestParam(required = false) Size size,
                              RedirectAttributes flash) {
        ProductRequest request = new ProductRequest();
        request.setName(name);
        request.setDescription(description);
        request.setPrice(price);
        request.setQuantity(quantity);
        request.setCategoryId(categoryId);
        request.setSize(size);
        productService.updateProduct(id, request);
        flash.addFlashAttribute("success", "Product updated successfully!");
        return "redirect:/admin/products";
    }

    @PostMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes flash) {
        productService.deleteProduct(id);
        flash.addFlashAttribute("success", "Product deleted successfully!");
        return "redirect:/admin/products";
    }
}