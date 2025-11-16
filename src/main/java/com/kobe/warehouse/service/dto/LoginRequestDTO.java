package com.kobe.warehouse.service.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for login request containing username and password.
 */
public record LoginRequestDTO(
    @NotBlank(message = "Username is required") String username,

    @NotBlank(message = "Password is required") String password
) {}
