package com.kobe.warehouse.config;

import static org.springframework.security.config.Customizer.withDefaults;

import com.kobe.warehouse.security.AuthoritiesConstants;
import com.kobe.warehouse.web.filter.SpaWebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.Supplier;
import org.springframework.boot.security.autoconfigure.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.util.StringUtils;

@Configuration
@EnableAsync
@EnableScheduling
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {

    private final LogProperties logProperties;

    private final RememberMeServices rememberMeServices;

    public SecurityConfiguration(RememberMeServices rememberMeServices, LogProperties logProperties) {
        this.rememberMeServices = rememberMeServices;
        this.logProperties = logProperties;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(withDefaults())
            .csrf(csrf ->
                csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
            )
            .addFilterAfter(new SpaWebFilter(), BasicAuthenticationFilter.class)
            .addFilterAfter(new CookieCsrfFilter(), BasicAuthenticationFilter.class)
            .headers(headers ->
                headers
                    .contentSecurityPolicy(csp -> csp.policyDirectives(logProperties.getSecurity().getContentSecurityPolicy()))
                    .frameOptions(FrameOptionsConfig::sameOrigin)
                    .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                    .permissionsPolicyHeader(permissions ->
                        permissions.policy(
                            "camera=(), fullscreen=(self), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), midi=(), payment=(), sync-xhr=()"
                        )
                    )
            )
            .securityMatcher(request -> !request.getRequestURI().startsWith("/java-client/"))
            .authorizeHttpRequests(authz ->
                authz
                    .requestMatchers(
                        PathRequest.toStaticResources().atCommonLocations(),
                        request -> request.getRequestURI().matches(".*\\.(js|txt|json|map|css|ttf|woff2|eot|woff|ico|png|svg|webapp)$"),
                        request -> request.getRequestURI().startsWith("/app/"),
                        request -> request.getRequestURI().startsWith("/i18n/"),
                        request -> request.getRequestURI().startsWith("/content/"),
                        request -> request.getRequestURI().startsWith("/swagger-ui/"),
                        request -> request.getRequestURI().equals("/api/authenticate"),
                        request -> request.getRequestURI().equals("/api/register"),
                        request -> request.getRequestURI().equals("/api/activate"),
                        request -> request.getRequestURI().equals("/api/account/reset-password/init"),
                        request -> request.getRequestURI().equals("/api/account/reset-password/finish"),
                        request -> request.getRequestURI().equals("/management/health"),
                        request -> request.getRequestURI().startsWith("/management/health/"),
                        request -> request.getRequestURI().equals("/management/info"),
                        request -> request.getRequestURI().equals("/api-user-account"),
                        request -> request.getRequestURI().equals("/management/prometheus"),
                        //  request -> request.getRequestURI().equals("/"),
                        request -> request.getRequestURI().equals("/index.html")
                    )
                    .permitAll()
                    .requestMatchers(
                        request -> request.getRequestURI().startsWith("/api/admin/"),
                        request -> request.getRequestURI().startsWith("/v3/api-docs/"),
                        request -> request.getRequestURI().startsWith("/management/")
                    )
                    .hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers(request -> request.getRequestURI().startsWith("/api/"))
                    .authenticated()
            )
            .rememberMe(rememberMe ->
                rememberMe
                    .rememberMeServices(rememberMeServices)
                    .rememberMeParameter("remember-me")
                    .key(logProperties.getSecurity().getRememberMe().getKey())
            )
            .exceptionHandling(exceptionHandling ->
                exceptionHandling.defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), request ->
                    request.getRequestURI().startsWith("/api/")
                )
            )
            .formLogin(formLogin ->
                formLogin
                    .loginPage("/")
                    .loginProcessingUrl("/api/authentication")
                    .successHandler((_, response, _) -> response.setStatus(HttpStatus.OK.value()))
                    .failureHandler((_, response, _) -> response.setStatus(HttpStatus.UNAUTHORIZED.value()))
                    .permitAll()
            )
            .logout(logout ->
                logout.logoutUrl("/api/logout").logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()).permitAll()
            );
        return http.build();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(AbstractHttpConfigurer::disable)
            .csrf(AbstractHttpConfigurer::disable)
            .securityMatchers(matchers -> matchers.requestMatchers("/java-client/**"))
            .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
            .httpBasic(withDefaults());
        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain securityFilterChainMobile(HttpSecurity http) throws Exception {
        http
            .cors(AbstractHttpConfigurer::disable)
            .csrf(AbstractHttpConfigurer::disable)
            .securityMatchers(matchers -> matchers.requestMatchers("/api-user-account"))
            .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());

        return http.build();
    }

    static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

        private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
        private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
            /*
             * Always use XorCsrfTokenRequestAttributeHandler to provide BREACH protection of
             * the CsrfToken when it is rendered in the response body.
             */
            this.xor.handle(request, response, csrfToken);

            // Render the token value to a cookie by causing the deferred token to be loaded.
            csrfToken.get();
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
                return this.plain.resolveCsrfTokenValue(request, csrfToken);
            }

            return this.xor.resolveCsrfTokenValue(request, csrfToken);
        }
    }
}
