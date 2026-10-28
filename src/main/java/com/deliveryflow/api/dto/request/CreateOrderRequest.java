package com.deliveryflow.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
    @NotBlank(message = "Pickup address is required")
        @jakarta.validation.constraints.Size(max = 500)
        String pickupAddress,
    @NotNull(message = "Pickup latitude is required")
        @jakarta.validation.constraints.DecimalMin("-90.0")
        @jakarta.validation.constraints.DecimalMax("90.0")
        Double pickupLat,
    @NotNull(message = "Pickup longitude is required")
        @jakarta.validation.constraints.DecimalMin("-180.0")
        @jakarta.validation.constraints.DecimalMax("180.0")
        Double pickupLng,
    @NotBlank(message = "Delivery address is required")
        @jakarta.validation.constraints.Size(max = 500)
        String deliveryAddress,
    @NotNull(message = "Delivery latitude is required")
        @jakarta.validation.constraints.DecimalMin("-90.0")
        @jakarta.validation.constraints.DecimalMax("90.0")
        Double deliveryLat,
    @NotNull(message = "Delivery longitude is required")
        @jakarta.validation.constraints.DecimalMin("-180.0")
        @jakarta.validation.constraints.DecimalMax("180.0")
        Double deliveryLng,
    @jakarta.validation.constraints.Size(max = 5000) String description) {}
