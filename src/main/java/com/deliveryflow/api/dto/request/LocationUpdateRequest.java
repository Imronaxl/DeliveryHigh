package com.deliveryflow.api.dto.request;

import jakarta.validation.constraints.NotNull;

public record LocationUpdateRequest(
    @NotNull(message = "Latitude is required")
        @jakarta.validation.constraints.DecimalMin("-90.0")
        @jakarta.validation.constraints.DecimalMax("90.0")
        Double latitude,
    @NotNull(message = "Longitude is required")
        @jakarta.validation.constraints.DecimalMin("-180.0")
        @jakarta.validation.constraints.DecimalMax("180.0")
        Double longitude) {}
