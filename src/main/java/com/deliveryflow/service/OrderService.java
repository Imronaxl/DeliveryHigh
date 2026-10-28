package com.deliveryflow.service;

import com.deliveryflow.api.exception.InvalidStatusTransitionException;
import com.deliveryflow.api.exception.OrderNotFoundException;
import com.deliveryflow.api.exception.UserNotFoundException;
import com.deliveryflow.domain.entity.Order;
import com.deliveryflow.domain.entity.OrderStatusHistory;
import com.deliveryflow.domain.enums.OrderStatus;
import com.deliveryflow.domain.enums.UserRole;
import com.deliveryflow.domain.repository.OrderRepository;
import com.deliveryflow.domain.repository.OrderStatusHistoryRepository;
import com.deliveryflow.domain.repository.UserRepository;
import com.deliveryflow.messaging.event.OrderCreatedEvent;
import com.deliveryflow.messaging.event.OrderStatusChangedEvent;
import com.deliveryflow.messaging.producer.OrderEventProducer;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

  private final OrderRepository orderRepository;
  private final UserRepository userRepository;
  private final OrderStatusHistoryRepository statusHistoryRepository;
  private final OrderEventProducer orderEventProducer;

  public OrderService(
      OrderRepository orderRepository,
      UserRepository userRepository,
      OrderStatusHistoryRepository statusHistoryRepository,
      OrderEventProducer orderEventProducer) {
    this.orderRepository = orderRepository;
    this.userRepository = userRepository;
    this.statusHistoryRepository = statusHistoryRepository;
    this.orderEventProducer = orderEventProducer;
  }

  @Transactional
  public Order createOrder(
      UUID clientId,
      String pickupAddress,
      String deliveryAddress,
      double pickupLat,
      double pickupLng,
      double deliveryLat,
      double deliveryLng,
      String description) {
    userRepository.findById(clientId).orElseThrow(() -> UserNotFoundException.byId(clientId));

    Order order =
        Order.builder()
            .clientId(clientId)
            .status(OrderStatus.CREATED)
            .pickupAddress(pickupAddress)
            .deliveryAddress(deliveryAddress)
            .pickupLat(pickupLat)
            .pickupLng(pickupLng)
            .deliveryLat(deliveryLat)
            .deliveryLng(deliveryLng)
            .description(description)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    Order savedOrder = orderRepository.save(order);

    OrderStatusHistory history =
        OrderStatusHistory.builder()
            .orderId(savedOrder.getId())
            .status(OrderStatus.CREATED)
            .changedAt(Instant.now())
            .changedById(clientId)
            .build();
    statusHistoryRepository.save(history);

    orderEventProducer.sendOrderCreated(
        new OrderCreatedEvent(
            savedOrder.getId(), clientId, pickupAddress, deliveryAddress, Instant.now()));

    return savedOrder;
  }

  @Transactional(readOnly = true)
  public Order findById(UUID orderId) {
    return orderRepository
        .findById(orderId)
        .orElseThrow(() -> OrderNotFoundException.byId(orderId));
  }

  @Transactional(readOnly = true)
  public List<Order> findByClientId(UUID clientId) {
    return orderRepository.findByClientId(clientId);
  }

  @Transactional(readOnly = true)
  public Page<Order> findAll(Pageable pageable) {
    return orderRepository.findAll(pageable);
  }

  @Transactional(readOnly = true)
  public Order findVisibleById(UUID orderId, UUID viewerId) {
    Order order = findById(orderId);
    var viewer =
        userRepository.findById(viewerId).orElseThrow(() -> UserNotFoundException.byId(viewerId));
    if (viewer.getRole() != UserRole.ADMIN
        && !viewerId.equals(order.getClientId())
        && !viewerId.equals(order.getCourierId())) {
      throw new AccessDeniedException("Order access denied");
    }
    return order;
  }

  private Order locked(UUID id) {
    return orderRepository.findLockedById(id).orElseThrow(() -> OrderNotFoundException.byId(id));
  }

  @Transactional
  public Order cancelOrder(UUID orderId, UUID clientId) {
    Order order = locked(orderId);
    if (!order.getClientId().equals(clientId)) {
      throw new AccessDeniedException("Only order creator can cancel");
    }
    updateStatus(order, OrderStatus.CANCELLED, clientId);
    return order;
  }

  @Transactional
  public Order assignCourier(UUID orderId, UUID courierId) {
    Order order = locked(orderId);
    var courier =
        userRepository.findById(courierId).orElseThrow(() -> UserNotFoundException.byId(courierId));
    if (courier.getRole() != UserRole.COURIER) {
      throw new AccessDeniedException("User is not a courier");
    }
    validateStatusTransition(order.getStatus(), OrderStatus.ASSIGNED);
    order.setCourierId(courierId);
    updateStatus(order, OrderStatus.ASSIGNED, courierId);
    return order;
  }

  @Transactional
  public Order updateStatus(UUID orderId, OrderStatus newStatus, UUID changedById) {
    Order order = locked(orderId);
    if (!changedById.equals(order.getCourierId())
        || !Set.of(OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT, OrderStatus.DELIVERED)
            .contains(newStatus)) {
      throw new AccessDeniedException("Courier action denied");
    }
    updateStatus(order, newStatus, changedById);
    return order;
  }

  private void updateStatus(Order order, OrderStatus newStatus, UUID changedById) {
    OrderStatus oldStatus = order.getStatus();
    validateStatusTransition(oldStatus, newStatus);

    order.setStatus(newStatus);
    order.setUpdatedAt(Instant.now());
    orderRepository.save(order);

    OrderStatusHistory history =
        OrderStatusHistory.builder()
            .orderId(order.getId())
            .status(newStatus)
            .changedAt(Instant.now())
            .changedById(changedById)
            .build();
    statusHistoryRepository.save(history);

    orderEventProducer.sendOrderStatusChanged(
        new OrderStatusChangedEvent(
            order.getId(), oldStatus, newStatus, changedById, Instant.now()));
  }

  private void validateStatusTransition(OrderStatus from, OrderStatus to) {
    boolean valid =
        switch (from) {
          case CREATED -> to == OrderStatus.ASSIGNED || to == OrderStatus.CANCELLED;
          case ASSIGNED -> to == OrderStatus.PICKED_UP || to == OrderStatus.CANCELLED;
          case PICKED_UP -> to == OrderStatus.IN_TRANSIT;
          case IN_TRANSIT -> to == OrderStatus.DELIVERED;
          default -> false;
        };

    if (!valid) {
      throw InvalidStatusTransitionException.fromTo(from, to);
    }
  }

  @Transactional(readOnly = true)
  public List<OrderStatusHistory> getStatusHistory(UUID orderId) {
    findById(orderId);
    return statusHistoryRepository.findByOrderIdOrderByChangedAtDesc(orderId);
  }
}
