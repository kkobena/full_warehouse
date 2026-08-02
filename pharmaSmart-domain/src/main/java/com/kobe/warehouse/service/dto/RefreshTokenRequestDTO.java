package com.kobe.warehouse.service.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for the refresh token request.
 */
public record RefreshTokenRequestDTO(
    @NotBlank(message = "Refresh token is required") String refreshToken
) {}
