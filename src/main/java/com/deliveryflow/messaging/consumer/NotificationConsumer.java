package com.deliveryflow.messaging.consumer;

import com.deliveryflow.messaging.event.NotificationSendEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {

  private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

  @KafkaListener(topics = "notifications.send", groupId = "notification-service-group")
  public void consumeNotification(NotificationSendEvent event) {
    log.info(
        "Received notification event for user {}: {} - {}",
        event.userId(),
        event.type(),
        event.message());
  }
}
