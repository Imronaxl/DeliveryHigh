package com.deliveryflow.messaging.consumer;

import com.deliveryflow.infrastructure.redis.CourierGeoRepository;
import com.deliveryflow.infrastructure.websocket.WebSocketNotificationService;
import com.deliveryflow.messaging.event.CourierLocationUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Point;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class LocationConsumer {

  private static final Logger log = LoggerFactory.getLogger(LocationConsumer.class);

  private final CourierGeoRepository courierGeoRepository;
  private final WebSocketNotificationService wsNotifications;

  public LocationConsumer(
      CourierGeoRepository courierGeoRepository, WebSocketNotificationService wsNotifications) {
    this.courierGeoRepository = courierGeoRepository;
    this.wsNotifications = wsNotifications;
  }

  @KafkaListener(topics = "courier.location-updated", groupId = "location-service-group")
  public void consumeLocationUpdate(CourierLocationUpdatedEvent event) {
    log.debug(
        "Location update for courier {}: [{}, {}]",
        event.courierId(),
        event.latitude(),
        event.longitude());

    boolean updated =
        courierGeoRepository.updateCourierLocation(
            event.courierId().toString(),
            new Point(event.longitude(), event.latitude()),
            event.timestamp());

    if (updated) wsNotifications.sendCourierLocationUpdate(event.courierId(), event);
  }
}
