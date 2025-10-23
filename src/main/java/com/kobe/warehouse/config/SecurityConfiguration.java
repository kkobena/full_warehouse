package com.kobe.warehouse.config;

import static org.springframework.security.config.Customizer.withDefaults;

import com.kobe.warehouse.security.AuthoritiesConstants;
import com.kobe.warehouse.security.jwt.JwtAuthenticationConverter;
import org.springframework.boot.security.autoconfigure.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;


@Configuration
@EnableAsync
@EnableScheduling
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {

    private final LogProperties logProperties;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    public SecurityConfiguration(
        LogProperties logProperties,
        JwtAuthenticationConverter jwtAuthenticationConverter
    ) {
        this.logProperties = logProperties;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(withDefaults())
            // Disable CSRF for stateless JWT authentication
            .csrf(AbstractHttpConfigurer::disable)
            // Stateless session management (no server-side sessions)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // Security headers
            .headers(headers ->
                headers
                    .contentSecurityPolicy(csp ->
                        csp.policyDirectives(logProperties.getSecurity().getContentSecurityPolicy())
                    )
                    .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                    .referrerPolicy(referrer ->
                        referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                    )
                    .permissionsPolicyHeader(permissions ->
                        permissions.policy(
                            "camera=(), fullscreen=(self), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), midi=(), payment=(), sync-xhr=()"
                        )
                    )
            )
            // Apply to all requests (including /java-client/*)
            .securityMatcher(request -> true)
            // Authorization rules
            .authorizeHttpRequests(authz ->
                authz
                    // Public endpoints
                    .requestMatchers(
                        PathRequest.toStaticResources().atCommonLocations(),
                        request -> request.getRequestURI().matches(".*\\.(js|txt|json|map|css|ttf|woff2|eot|woff|ico|png|svg|webapp)$"),
                        request -> request.getRequestURI().startsWith("/app/"),
                        request -> request.getRequestURI().startsWith("/i18n/"),
                        request -> request.getRequestURI().startsWith("/content/"),
                        request -> request.getRequestURI().startsWith("/swagger-ui/"),
                        request -> request.getRequestURI().startsWith("/v3/api-docs/"),
                        request -> request.getRequestURI().startsWith("/api/auth/"), // JWT login endpoint
                        request -> request.getRequestURI().equals("/api/register"),
                        request -> request.getRequestURI().equals("/api/activate"),
                        request -> request.getRequestURI().equals("/api/account/reset-password/init"),
                        request -> request.getRequestURI().equals("/api/account/reset-password/finish"),
                        request -> request.getRequestURI().equals("/management/health"),
                        request -> request.getRequestURI().startsWith("/management/health/"),
                        request -> request.getRequestURI().equals("/management/info"),

                        request -> request.getRequestURI().equals("/index.html"),
                        request -> request.getRequestURI().equals("/")
                    )
                    .permitAll()
                    // Admin-only endpoints (excluding public health/info endpoints)
                    .requestMatchers(
                        request -> request.getRequestURI().startsWith("/api/admin/"),
                        request -> {
                            String uri = request.getRequestURI();
                            return uri.startsWith("/management/")
                                && !uri.equals("/management/health")
                                && !uri.startsWith("/management/health/")
                             ;
                        }
                    )
                    .hasAuthority(AuthoritiesConstants.ADMIN)
                    // All other /api/** and /java-client/** endpoints require authentication
                    .requestMatchers(
                        request -> request.getRequestURI().startsWith("/api/"),
                        request -> request.getRequestURI().startsWith("/java-client/")
                    )
                    .authenticated()
            )
            // Return 401 Unauthorized for unauthenticated API requests
            .exceptionHandling(exceptionHandling ->
                exceptionHandling.defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    request -> request.getRequestURI().startsWith("/api/")
                )
            )
            // OAuth2 Resource Server: Validate JWT tokens
            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)
                )
            );
        return http.build();
    }

}
