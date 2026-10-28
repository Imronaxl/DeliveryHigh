package com.deliveryflow.messaging.event;

import com.deliveryflow.domain.enums.OrderStatus;
import java.time.Instant;
import java.util.UUID;

public record OrderStatusChangedEvent(
    UUID orderId,
    OrderStatus oldStatus,
    OrderStatus newStatus,
    UUID changedById,
    Instant changedAt) {}
