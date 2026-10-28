package com.deliveryflow.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id, String email, String name, String phone, String role, Instant createdAt) {}
