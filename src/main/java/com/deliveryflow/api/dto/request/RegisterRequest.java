package com.deliveryflow.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 255)
        String email,
    @NotBlank(message = "Password is required")
        @Size(min = 6, max = 72, message = "Password must be at least 6 characters")
        String password,
    @NotBlank(message = "Name is required") @Size(max = 255) String name,
    @Size(max = 50) String phone,
    @NotBlank(message = "Role is required") String role) {}
