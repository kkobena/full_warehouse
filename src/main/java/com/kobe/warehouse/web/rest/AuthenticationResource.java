package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.service.JwtService;
import com.kobe.warehouse.service.dto.JwtTokenDTO;
import com.kobe.warehouse.service.dto.LoginRequestDTO;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication using JWT tokens.
 *
 * Flow:
 * 1. Client sends POST to /api/auth/login with username/password
 * 2. Backend validates credentials against database
 * 3. If valid, generate JWT access token and refresh token
 * 4. Return tokens to client
 * 5. Client stores tokens and includes access token in Authorization header for subsequent requests
 */
@RestController
@RequestMapping("/api/auth")
public class AuthenticationResource {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationResource.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthenticationResource(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Authenticate user with username/password and return JWT tokens.
     *
     * POST /api/auth/login
     *
     * @param loginRequest Login credentials
     * @return JWT access token and refresh token
     */
    @PostMapping("/login")
    public ResponseEntity<JwtTokenDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        try {
            // Authenticate user credentials against database
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.username(),
                    loginRequest.password()
                )
            );

            // Generate JWT tokens
            String accessToken = jwtService.generateAccessToken(authentication, 8); // 8 hours
            String refreshToken = jwtService.generateRefreshToken(authentication, 30); // 30 days

            JwtTokenDTO tokenResponse = new JwtTokenDTO(
                accessToken,
                refreshToken,
                8 * 3600 // expiration in seconds
            );

            log.debug("User {} authenticated successfully", loginRequest.username());
            return ResponseEntity.ok(tokenResponse);

        } catch (BadCredentialsException e) {
            log.debug("Authentication failed for user: {}", loginRequest.username());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Refresh access token using refresh token.
     *
     * POST /api/auth/refresh
     *
     * @param refreshToken Refresh token from previous login
     * @return New JWT access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<JwtTokenDTO> refresh(@RequestBody String refreshToken) {
        // TODO: Implement refresh token validation and new access token generation
        // For now, return 501 Not Implemented
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
