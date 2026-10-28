package com.deliveryflow.api.dto.request;

import com.deliveryflow.domain.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull(message = "Status is required") OrderStatus status) {}
