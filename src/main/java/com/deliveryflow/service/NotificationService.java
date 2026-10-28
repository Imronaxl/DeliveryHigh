package com.deliveryflow.service;

import com.deliveryflow.messaging.event.NotificationSendEvent;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

  private final com.deliveryflow.messaging.producer.NotificationProducer producer;

  public NotificationService(com.deliveryflow.messaging.producer.NotificationProducer producer) {
    this.producer = producer;
  }

  public void sendNotification(NotificationSendEvent event) {
    producer.sendNotification(event);
  }
}
