package com.kobe.warehouse.security;

import com.kobe.warehouse.config.Constants;
import com.kobe.warehouse.domain.Authority;
import com.kobe.warehouse.domain.Menu;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.CollectionUtils;

/**
 * Utility class for Spring Security.
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * Get the login of the current user.
     *
     * @return the login of the current user.
     */
    public static Optional<String> getCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(extractPrincipal(securityContext.getAuthentication()));
    }

    private static String extractPrincipal(Authentication authentication) {
        if (authentication == null) {
            return null;
        } else if (authentication.getPrincipal() instanceof UserDetails springSecurityUser) {
            return springSecurityUser.getUsername();
        } else if (authentication.getPrincipal() instanceof String s) {
            return s;
        } else if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {

            return jwt.getSubject();
        }

        return authentication.getName();
    }

    /**
     * Check if a user is authenticated.
     *
     * @return true if the user is authenticated, false otherwise.
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && getAuthorities(authentication).noneMatch(AuthoritiesConstants.ANONYMOUS::equals);
    }

    /**
     * Checks if the current user has any of the authorities.
     *
     * @param authorities the authorities to check.
     * @return true if the current user has any of the authorities, false otherwise.
     */
    public static boolean hasCurrentUserAnyOfAuthorities(String... authorities) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (
            authentication != null && getAuthorities(authentication).anyMatch(authority -> Arrays.asList(authorities).contains(authority))
        );
    }

    /**
     * Checks if the current user has none of the authorities.
     *
     * @param authorities the authorities to check.
     * @return true if the current user has none of the authorities, false otherwise.
     */
    public static boolean hasCurrentUserNoneOfAuthorities(String... authorities) {
        return !hasCurrentUserAnyOfAuthorities(authorities);
    }

    /**
     * Checks if the current user has a specific authority.
     *
     * @param authority the authority to check.
     * @return true if the current user has the authority, false otherwise.
     */
    public static boolean hasCurrentUserThisAuthority(String authority) {
        return hasCurrentUserAnyOfAuthorities(authority);
    }

    private static Stream<String> getAuthorities(Authentication authentication) {
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority);
    }

    private static Set<String> buildAuthorities(Authority authority) {
        Set<String> authorities = new HashSet<>();
        authorities.add(authority.getName());
        authorities.addAll(authority.getMenus().stream().map(Menu::getName).collect(Collectors.toSet()));
        authorities.addAll(authority.getPrivileges().stream().map(action -> action.getPrivilege().getName()).collect(Collectors.toSet()));
        return authorities;
    }

    public static Set<String> mergeAuthorities(Set<Authority> authorities) {
        if (CollectionUtils.isEmpty(authorities)) {
            return Collections.emptySet();
        }
        Set<String> authorities0 = new HashSet<>();
        authorities.forEach(authority -> authorities0.addAll(buildAuthorities(authority)));
        return authorities0;
    }

    public static boolean hasMobileAccess(String authority) {
        return (
            Constants.PR_MOBILE_ADMIN.equals(authority) ||
            Constants.PR_MOBILE_USER.equals(authority) ||
            Constants.ROLE_ADMIN.equals(authority)
        );
    }

    public static boolean isAdmin(String authority) {
        return Constants.ROLE_ADMIN.equals(authority);
    }

    public static boolean hasMobileAdminAccess(String authority) {
        return Constants.PR_MOBILE_ADMIN.equals(authority);
    }

    public static boolean hasUserMobileAccess(String authority) {
        return Constants.PR_MOBILE_USER.equals(authority);
    }
}
