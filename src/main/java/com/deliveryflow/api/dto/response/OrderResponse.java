package com.deliveryflow.api.dto.response;

import com.deliveryflow.domain.enums.OrderStatus;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    UUID clientId,
    UUID courierId,
    OrderStatus status,
    String pickupAddress,
    String deliveryAddress,
    Double pickupLat,
    Double pickupLng,
    Double deliveryLat,
    Double deliveryLng,
    String description,
    Instant createdAt,
    Instant updatedAt) {}
