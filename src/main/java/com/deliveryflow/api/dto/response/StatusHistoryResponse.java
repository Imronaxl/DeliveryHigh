package com.deliveryflow.api.dto.response;

import com.deliveryflow.domain.enums.OrderStatus;
import java.time.Instant;
import java.util.UUID;

public record StatusHistoryResponse(
    UUID id, UUID orderId, OrderStatus status, Instant changedAt, UUID changedById) {}
