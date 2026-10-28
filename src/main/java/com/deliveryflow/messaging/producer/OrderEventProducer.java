package com.deliveryflow.messaging.producer;

import com.deliveryflow.config.KafkaConfig;
import com.deliveryflow.messaging.event.OrderCreatedEvent;
import com.deliveryflow.messaging.event.OrderStatusChangedEvent;
import org.springframework.stereotype.Component;

@Component
public class OrderEventProducer {

  private final EventOutbox outbox;

  public OrderEventProducer(EventOutbox outbox) {
    this.outbox = outbox;
  }

  public void sendOrderCreated(OrderCreatedEvent event) {
    outbox.append(KafkaConfig.ORDERS_CREATED, event.orderId().toString(), event);
  }

  public void sendOrderStatusChanged(OrderStatusChangedEvent event) {
    outbox.append(KafkaConfig.ORDERS_STATUS_CHANGED, event.orderId().toString(), event);
  }
}
