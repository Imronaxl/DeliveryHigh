package com.deliveryflow.api.controller;

import com.deliveryflow.api.dto.request.LocationUpdateRequest;
import com.deliveryflow.api.dto.response.OrderResponse;
import com.deliveryflow.api.dto.request.UpdateStatusRequest;
import com.deliveryflow.domain.entity.Order;
import com.deliveryflow.mapper.OrderMapper;
import com.deliveryflow.service.CourierService;
import com.deliveryflow.service.LocationService;
import com.deliveryflow.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courier")
public class CourierController {

    private final CourierService courierService;
    private final OrderService orderService;
    private final LocationService locationService;
    private final OrderMapper orderMapper;

    public CourierController(
            CourierService courierService,
            OrderService orderService,
            LocationService locationService,
            OrderMapper orderMapper) {
        this.courierService = courierService;
        this.orderService = orderService;
        this.locationService = locationService;
        this.orderMapper = orderMapper;
    }

    @GetMapping("/orders/available")
    public ResponseEntity<List<OrderResponse>> getAvailableOrders() {
        List<Order> orders = courierService.findAvailableOrders();
        return ResponseEntity.ok(orders.stream().map(orderMapper::toResponse).toList());
    }

    @GetMapping("/orders/active")
    public ResponseEntity<List<OrderResponse>> getActiveOrders(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID courierId = UUID.fromString(userDetails.getUsername());
        List<Order> orders = courierService.findActiveOrders(courierId);
        return ResponseEntity.ok(orders.stream().map(orderMapper::toResponse).toList());
    }

    @PatchMapping("/orders/{id}/accept")
    public ResponseEntity<OrderResponse> acceptOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID courierId = UUID.fromString(userDetails.getUsername());
        Order order = orderService.assignCourier(id, courierId);
        return ResponseEntity.ok(orderMapper.toResponse(order));
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID courierId = UUID.fromString(userDetails.getUsername());
        Order order = orderService.updateStatus(id, request.status(), courierId);
        return ResponseEntity.ok(orderMapper.toResponse(order));
    }

    @PostMapping("/location")
    public ResponseEntity<Void> updateLocation(
            @Valid @RequestBody LocationUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID courierId = UUID.fromString(userDetails.getUsername());
        locationService.updateCourierLocation(courierId, request.latitude(), request.longitude());
        return ResponseEntity.noContent().build();
    }
}
