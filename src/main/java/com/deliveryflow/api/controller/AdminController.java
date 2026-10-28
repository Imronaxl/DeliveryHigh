package com.deliveryflow.api.controller;

import com.deliveryflow.api.dto.response.OrderResponse;
import com.deliveryflow.api.dto.response.StatusHistoryResponse;
import com.deliveryflow.api.dto.response.UserResponse;
import com.deliveryflow.mapper.OrderMapper;
import com.deliveryflow.mapper.UserMapper;
import com.deliveryflow.service.CourierService;
import com.deliveryflow.service.OrderService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

  private final OrderService orderService;
  private final CourierService courierService;
  private final OrderMapper orderMapper;
  private final UserMapper userMapper;

  @GetMapping("/orders")
  public Page<OrderResponse> getAllOrders(Pageable pageable) {
    return orderService.findAll(pageable).map(orderMapper::toResponse);
  }

  @GetMapping("/couriers")
  public List<UserResponse> getAllCouriers() {
    return userMapper.toResponseList(courierService.findAllCouriers());
  }

  @GetMapping("/orders/{id}/history")
  public List<StatusHistoryResponse> getOrderHistory(@PathVariable UUID id) {
    return orderMapper.toHistoryResponseList(orderService.getStatusHistory(id));
  }
}
