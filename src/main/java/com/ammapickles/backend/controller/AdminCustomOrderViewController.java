package com.ammapickles.backend.controller;

import com.ammapickles.backend.entity.ChatMessage;
import com.ammapickles.backend.entity.CustomOrderRequest;
import com.ammapickles.backend.entity.CustomOrderStatus;
import com.ammapickles.backend.repository.CustomOrderRequestRepository;
import com.ammapickles.backend.repository.UserRepository;
import com.ammapickles.backend.service.CustomPickleChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

/**
 * Admin controller for managing custom pickle order requests.
 * Admin can view, filter, update status, confirm with pricing, and add notes.
 */
@Controller
@RequestMapping("/admin/custom-orders")
@RequiredArgsConstructor
public class AdminCustomOrderViewController {

    private final CustomOrderRequestRepository customOrderRepo;
    private final CustomPickleChatService chatService;
    private final UserRepository userRepository;

    /**
     * Show all custom order requests with optional status filter.
     */
    @GetMapping
    public String listCustomOrders(@RequestParam(required = false) String status,
                                   Model model) {
        List<CustomOrderRequest> requests;

        if (status != null && !status.isEmpty()) {
            try {
                CustomOrderStatus filterStatus = CustomOrderStatus.valueOf(status.toUpperCase());
                requests = customOrderRepo.findByStatusOrderByCreatedAtDesc(filterStatus);
            } catch (IllegalArgumentException e) {
                requests = customOrderRepo.findAllByOrderByCreatedAtDesc();
            }
        } else {
            requests = customOrderRepo.findAllByOrderByCreatedAtDesc();
        }

        model.addAttribute("requests", requests);
        model.addAttribute("currentFilter", status);
        model.addAttribute("newCount", customOrderRepo.countByStatus(CustomOrderStatus.NEW));
        model.addAttribute("contactedCount", customOrderRepo.countByStatus(CustomOrderStatus.CONTACTED));
        model.addAttribute("confirmedCount", customOrderRepo.countByStatus(CustomOrderStatus.CONFIRMED));
        model.addAttribute("preparingCount", customOrderRepo.countByStatus(CustomOrderStatus.PREPARING));
        model.addAttribute("completedCount", customOrderRepo.countByStatus(CustomOrderStatus.COMPLETED));
        model.addAttribute("cancelledCount", customOrderRepo.countByStatus(CustomOrderStatus.CANCELLED));

        return "admin/custom-orders";
    }

    /**
     * View details of a specific custom order, including chat history.
     */
    @GetMapping("/{id}")
    public String viewCustomOrder(@PathVariable Long id, Model model) {
        CustomOrderRequest request = customOrderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Custom order not found"));

        List<ChatMessage> chatHistory = chatService.getChatHistory(request.getSessionId());

        model.addAttribute("request", request);
        model.addAttribute("chatHistory", chatHistory);
        return "admin/custom-order-detail";
    }

    /**
     * Update the status of a custom order request.
     */
    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam String status,
                               RedirectAttributes redirectAttributes) {
        CustomOrderRequest request = customOrderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Custom order not found"));

        try {
            CustomOrderStatus newStatus = CustomOrderStatus.valueOf(status.toUpperCase());
            request.setStatus(newStatus);
            customOrderRepo.save(request);
            redirectAttributes.addFlashAttribute("success",
                    "Status updated to " + newStatus.name());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Invalid status");
        }

        return "redirect:/admin/custom-orders/" + id;
    }

    /**
     * Confirm a custom order with agreed price and advance payment details.
     * Admin fills this after speaking to customer on phone.
     */
    @PostMapping("/{id}/confirm")
    public String confirmOrder(@PathVariable Long id,
                               @RequestParam BigDecimal agreedPrice,
                               @RequestParam(required = false, defaultValue = "0") BigDecimal advancePaid,
                               @RequestParam(required = false, defaultValue = "") String paymentReference,
                               @RequestParam(required = false, defaultValue = "") String estimatedDelivery,
                               @RequestParam(required = false, defaultValue = "") String deliveryAddress,
                               @RequestParam(required = false, defaultValue = "") String notes,
                               RedirectAttributes redirectAttributes) {
        CustomOrderRequest request = customOrderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Custom order not found"));

        request.setAgreedPrice(agreedPrice);
        request.setAdvancePaid(advancePaid);
        request.setBalanceDue(agreedPrice.subtract(advancePaid));
        request.setPaymentReference(paymentReference.isBlank() ? null : paymentReference);
        request.setEstimatedDelivery(estimatedDelivery.isBlank() ? null : estimatedDelivery);
        request.setDeliveryAddress(deliveryAddress.isBlank() ? null : deliveryAddress);
        if (!notes.isBlank()) {
            request.setAdminNotes(notes);
        }
        request.setStatus(CustomOrderStatus.CONFIRMED);

        // Try to auto-link customer's user account by phone if not already linked
        if (request.getUser() == null && request.getPhoneNumber() != null) {
            String phone = request.getPhoneNumber().replaceAll("[^0-9]", "");
            if (phone.length() == 10) {
                userRepository.findByPhoneNumber(phone).ifPresent(request::setUser);
            }
        }

        customOrderRepo.save(request);
        redirectAttributes.addFlashAttribute("success",
                "✅ Order confirmed! Agreed ₹" + agreedPrice + ", Advance ₹" + advancePaid +
                ", Balance ₹" + request.getBalanceDue());

        return "redirect:/admin/custom-orders/" + id;
    }

    /**
     * Add or update admin notes for a custom order.
     */
    @PostMapping("/{id}/notes")
    public String updateNotes(@PathVariable Long id,
                              @RequestParam String notes,
                              RedirectAttributes redirectAttributes) {
        CustomOrderRequest request = customOrderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Custom order not found"));

        request.setAdminNotes(notes);
        customOrderRepo.save(request);
        redirectAttributes.addFlashAttribute("success", "Notes saved successfully");

        return "redirect:/admin/custom-orders/" + id;
    }
}
