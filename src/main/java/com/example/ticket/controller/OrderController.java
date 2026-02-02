package com.example.ticket.controller;

import com.example.ticket.dto.request.CreateOrderRequest;
import com.example.ticket.dto.response.ApiResponse;
import com.example.ticket.dto.response.OrderResponse;
import com.example.ticket.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for order-related operations.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final com.example.ticket.domain.repository.UserRepository userRepository; // Inject UserRepository

    /**
     * Create a new order (hold tickets and get payment URL).
     * This is Phase 1: Hold ticket in high traffic.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {
        try {
            // Get authenticated user
            String email = org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            com.example.ticket.domain.entity.User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String ipAddress = getClientIp(httpRequest);
            // Pass userId to service
            OrderResponse order = orderService.createOrder(request, ipAddress, user.getId());
            return ResponseEntity.ok(ApiResponse.success("Order created successfully", order));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get order by order code.
     */
    @GetMapping("/{orderCode}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable String orderCode) {
        try {
            OrderResponse order = orderService.getOrderByCode(orderCode);
            return ResponseEntity.ok(ApiResponse.success(order));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get client IP address.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
