package com.deliveryflow.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.deliveryflow.api.exception.InvalidStatusTransitionException;
import com.deliveryflow.domain.entity.*;
import com.deliveryflow.domain.enums.*;
import com.deliveryflow.domain.repository.*;
import com.deliveryflow.messaging.producer.OrderEventProducer;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
  @Mock OrderRepository orders;
  @Mock UserRepository users;
  @Mock OrderStatusHistoryRepository history;
  @Mock OrderEventProducer events;
  @InjectMocks OrderService service;
  UUID client = UUID.randomUUID(), courier = UUID.randomUUID(), id = UUID.randomUUID();
  Order order;

  @BeforeEach
  void setup() {
    order = Order.builder().id(id).clientId(client).status(OrderStatus.CREATED).build();
  }

  @Test
  void createsOrderAndHistory() {
    when(users.findById(client))
        .thenReturn(Optional.of(User.builder().id(client).role(UserRole.CLIENT).build()));
    when(orders.save(any()))
        .thenAnswer(
            call -> {
              Order value = call.getArgument(0);
              value.setId(id);
              return value;
            });
    Order result = service.createOrder(client, "A", "B", 55, 37, 56, 38, "Parcel");
    assertThat(result.getStatus()).isEqualTo(OrderStatus.CREATED);
    var entry = ArgumentCaptor.forClass(OrderStatusHistory.class);
    verify(history).save(entry.capture());
    assertThat(entry.getValue().getOrderId()).isEqualTo(id);
    verify(events).sendOrderCreated(any());
  }

  @Test
  void assignsOnlyCreatedOrder() {
    when(orders.findLockedById(id)).thenReturn(Optional.of(order));
    when(users.findById(courier))
        .thenReturn(Optional.of(User.builder().id(courier).role(UserRole.COURIER).build()));
    service.assignCourier(id, courier);
    assertThat(order.getCourierId()).isEqualTo(courier);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.ASSIGNED);
    assertThatThrownBy(() -> service.assignCourier(id, courier))
        .isInstanceOf(InvalidStatusTransitionException.class);
  }

  @Test
  void rejectsAnotherCourier() {
    order.setCourierId(courier);
    order.setStatus(OrderStatus.ASSIGNED);
    when(orders.findLockedById(id)).thenReturn(Optional.of(order));
    assertThatThrownBy(() -> service.updateStatus(id, OrderStatus.PICKED_UP, UUID.randomUUID()))
        .isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(events, history);
  }

  @Test
  void rejectsSkippedStatus() {
    order.setCourierId(courier);
    order.setStatus(OrderStatus.ASSIGNED);
    when(orders.findLockedById(id)).thenReturn(Optional.of(order));
    assertThatThrownBy(() -> service.updateStatus(id, OrderStatus.DELIVERED, courier))
        .isInstanceOf(InvalidStatusTransitionException.class);
  }

  @Test
  void completesDeliveryInOrder() {
    order.setCourierId(courier);
    order.setStatus(OrderStatus.ASSIGNED);
    when(orders.findLockedById(id)).thenReturn(Optional.of(order));
    for (var status : List.of(OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT, OrderStatus.DELIVERED))
      service.updateStatus(id, status, courier);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    verify(history, times(3)).save(any());
    verify(events, times(3)).sendOrderStatusChanged(any());
  }

  @Test
  void creatorCanCancelBeforePickup() {
    when(orders.findLockedById(id)).thenReturn(Optional.of(order));
    service.cancelOrder(id, client);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
  }

  @Test
  void rejectsCancellationAfterPickup() {
    order.setStatus(OrderStatus.PICKED_UP);
    when(orders.findLockedById(id)).thenReturn(Optional.of(order));
    assertThatThrownBy(() -> service.cancelOrder(id, client))
        .isInstanceOf(InvalidStatusTransitionException.class);
  }

  @Test
  void rejectsCancellationByStranger() {
    when(orders.findLockedById(id)).thenReturn(Optional.of(order));
    assertThatThrownBy(() -> service.cancelOrder(id, UUID.randomUUID()))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void deniesReadingAnotherClientsOrder() {
    UUID stranger = UUID.randomUUID();
    when(orders.findById(id)).thenReturn(Optional.of(order));
    when(users.findById(stranger))
        .thenReturn(Optional.of(User.builder().id(stranger).role(UserRole.CLIENT).build()));
    assertThatThrownBy(() -> service.findVisibleById(id, stranger))
        .isInstanceOf(AccessDeniedException.class);
  }
}
