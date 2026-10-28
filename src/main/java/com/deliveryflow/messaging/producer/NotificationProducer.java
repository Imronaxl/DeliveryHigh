package com.deliveryflow.messaging.producer;

import com.deliveryflow.config.KafkaConfig;
import com.deliveryflow.messaging.event.CourierLocationUpdatedEvent;
import com.deliveryflow.messaging.event.NotificationSendEvent;
import org.springframework.stereotype.Component;

@Component
public class NotificationProducer {

  private final EventOutbox outbox;

  public NotificationProducer(EventOutbox outbox) {
    this.outbox = outbox;
  }

  public void sendNotification(NotificationSendEvent event) {
    outbox.append(KafkaConfig.NOTIFICATIONS_SEND, event.userId().toString(), event);
  }

  public void sendLocationUpdate(CourierLocationUpdatedEvent event) {
    outbox.append(KafkaConfig.COURIER_LOCATION_UPDATED, event.courierId().toString(), event);
  }
}
