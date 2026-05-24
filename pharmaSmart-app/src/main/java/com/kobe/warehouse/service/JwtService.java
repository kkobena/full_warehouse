package com.kobe.warehouse.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Service for generating JWT tokens after successful authentication.
 *
 * JWT tokens include:
 * - Standard claims: iss (issuer), iat (issued at), exp (expiration), sub (subject/username)
 * - Custom claims:
 *   - authorities: List of all user authorities (roles, menus, privileges) from DomainUserDetailsService
 *   - scope: Space-separated authorities for backward compatibility
 */
@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;

    public JwtService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    /**
     * Generate access token for authenticated user.
     *
     * The token includes:
     * - "sub" claim: username (from authentication.getName())
     * - "authorities" claim: List of authority strings (roles, menus, privileges)
     * - "scope" claim: Space-separated authorities (for backward compatibility)
     * - "iat" claim: Token issue time
     * - "exp" claim: Token expiration time
     *
     * The authorities are already merged by DomainUserDetailsService using SecurityUtils.mergeAuthorities(),
     * which flattens Authority roles, Menu names, and Privilege names into a single collection.
     *
     * @param authentication Spring Security authentication object containing user and authorities
     * @param expirationHours Token validity in hours
     * @return JWT access token
     */
    public String generateAccessToken(Authentication authentication, long expirationHours) {
        Instant now = Instant.now();

        // Extract authorities as a list for the "authorities" claim
        List<String> authorities = authentication
            .getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());

        // Also create space-separated scope for backward compatibility
        String scope = String.join(" ", authorities);

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("http://localhost:8080")
            .issuedAt(now)
            .expiresAt(now.plus(expirationHours, ChronoUnit.HOURS))
            .subject(authentication.getName())
            .claim("authorities", authorities) // List of authority strings
            .claim("scope", scope) // Space-separated for backward compatibility
            .build();

        return this.jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /**
     * Generate refresh token for authenticated user.
     * Refresh tokens have longer validity and can be used to obtain new access tokens.
     *
     * @param authentication Spring Security authentication object
     * @param expirationDays Token validity in days
     * @return JWT refresh token
     */
    public String generateRefreshToken(Authentication authentication, long expirationDays) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("http://localhost:8080")
            .issuedAt(now)
            .expiresAt(now.plus(expirationDays, ChronoUnit.DAYS))
            .subject(authentication.getName())
            .claim("token_type", "refresh")
            .build();

        return this.jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
