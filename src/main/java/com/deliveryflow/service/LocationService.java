package com.deliveryflow.service;

import com.deliveryflow.domain.enums.UserRole;
import com.deliveryflow.domain.repository.UserRepository;
import com.deliveryflow.messaging.event.CourierLocationUpdatedEvent;
import com.deliveryflow.messaging.producer.NotificationProducer;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocationService {

  private final UserRepository userRepository;
  private final NotificationProducer notificationProducer;

  public LocationService(UserRepository userRepository, NotificationProducer notificationProducer) {
    this.userRepository = userRepository;
    this.notificationProducer = notificationProducer;
  }

  @Transactional
  public void updateCourierLocation(UUID courierId, double latitude, double longitude) {
    userRepository
        .findById(courierId)
        .filter(user -> user.getRole() == UserRole.COURIER)
        .orElseThrow(() -> new IllegalArgumentException("User is not a courier"));

    notificationProducer.sendLocationUpdate(
        new CourierLocationUpdatedEvent(courierId, latitude, longitude, Instant.now()));
  }
}
