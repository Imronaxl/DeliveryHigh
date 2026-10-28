package com.deliveryflow.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AuthResponse(
    UUID userId,
    String email,
    String name,
    String role,
    String accessToken,
    String refreshToken,
    Instant createdAt) {}
