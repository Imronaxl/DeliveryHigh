package com.deliveryflow.messaging.consumer;

import com.deliveryflow.infrastructure.websocket.WebSocketNotificationService;
import com.deliveryflow.messaging.event.OrderCreatedEvent;
import com.deliveryflow.messaging.event.OrderStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

  private final WebSocketNotificationService wsNotifications;

  public OrderEventConsumer(WebSocketNotificationService wsNotifications) {
    this.wsNotifications = wsNotifications;
  }

  @KafkaListener(topics = "orders.created", groupId = "order-service-group")
  public void consumeOrderCreated(OrderCreatedEvent event) {
    log.info("Order created: {}", event.orderId());
    wsNotifications.broadcastNewOrder(event);
    wsNotifications.broadcastAdminOrder(event);
  }

  @KafkaListener(topics = "orders.status-changed", groupId = "order-service-group")
  public void consumeOrderStatusChanged(OrderStatusChangedEvent event) {
    log.info(
        "Order {} status changed: {} -> {}", event.orderId(), event.oldStatus(), event.newStatus());
    wsNotifications.broadcastAdminOrder(event);
    wsNotifications.sendOrderStatusUpdate(event.orderId(), event);
    if (event.oldStatus() == com.deliveryflow.domain.enums.OrderStatus.CREATED) {
      wsNotifications.broadcastNewOrder(event);
    }
  }
}
