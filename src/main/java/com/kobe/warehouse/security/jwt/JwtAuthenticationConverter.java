package com.kobe.warehouse.security.jwt;

import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Custom JWT Authentication Converter that extracts user authorities from JWT claims.
 *
 * This converter:
 * 1. Validates the JWT token (signature, expiration) - done by JwtDecoder before this
 * 2. Extracts username from "sub" claim
 * 3. Extracts authorities from "authorities" claim (array of authority strings)
 * 4. Creates Spring Security Authentication with proper authorities
 *
 * The authorities in the JWT include:
 * - Role names (e.g., "ROLE_ADMIN", "ROLE_USER")
 * - Menu names (e.g., "MENU_CUSTOMERS", "MENU_SALES")
 * - Privilege names (e.g., "PRIVILEGE_CREATE_CUSTOMER", "PRIVILEGE_VIEW_REPORTS")
 */
@Component
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    /**
     * Convert JWT to Spring Security Authentication.
     *
     * @param jwt Validated JWT token
     * @return Authentication object with user details and authorities
     */
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // Extract authorities from JWT claims
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        // Create authentication token with username and authorities
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    /**
     * Extract authorities from JWT "authorities" claim.
     *
     * The "authorities" claim contains a list of strings representing:
     * - Roles (from Authority table)
     * - Menus (from Menu table via Authority)
     * - Privileges (from Privilege table via Authority)
     *
     * These were merged by SecurityUtils.mergeAuthorities() during token generation.
     */
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Collection<String> authorities = jwt.getClaimAsStringList("authorities");

        if (authorities == null || authorities.isEmpty()) {
            // Fallback to "scope" claim for backward compatibility
            String scope = jwt.getClaimAsString("scope");
            if (scope != null && !scope.isEmpty()) {
                authorities = java.util.Arrays.asList(scope.split(" "));
            } else {
                return java.util.Collections.emptyList();
            }
        }

        return authorities
            .stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
    }
}
