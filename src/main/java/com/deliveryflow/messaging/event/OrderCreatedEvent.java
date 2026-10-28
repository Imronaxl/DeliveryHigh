package com.deliveryflow.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
    UUID orderId, UUID clientId, String pickupAddress, String deliveryAddress, Instant createdAt) {}
