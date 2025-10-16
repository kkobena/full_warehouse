package com.kobe.warehouse.service.dto;

/**
 * DTO for returning JWT tokens after successful authentication.
 */
public record JwtTokenDTO(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn
) {
    /**
     * Create JWT token response with Bearer token type.
     *
     * @param accessToken JWT access token
     * @param refreshToken JWT refresh token
     * @param expiresIn Token expiration time in seconds
     */
    public JwtTokenDTO(String accessToken, String refreshToken, long expiresIn) {
        this(accessToken, refreshToken, "Bearer", expiresIn);
    }
}
