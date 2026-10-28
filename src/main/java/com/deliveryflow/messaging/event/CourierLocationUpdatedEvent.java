package com.deliveryflow.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record CourierLocationUpdatedEvent(
    UUID courierId, double latitude, double longitude, Instant timestamp) {}
