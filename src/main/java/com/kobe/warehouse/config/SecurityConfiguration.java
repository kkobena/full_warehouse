package com.kobe.warehouse.config;

import static org.springframework.security.config.Customizer.withDefaults;

import com.kobe.warehouse.security.AuthoritiesConstants;
import com.kobe.warehouse.web.filter.SpaWebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
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
import tech.jhipster.config.JHipsterProperties;
import tech.jhipster.web.filter.CookieCsrfFilter;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {

    private final JHipsterProperties jHipsterProperties;

    private final RememberMeServices rememberMeServices;

    public SecurityConfiguration(RememberMeServices rememberMeServices, JHipsterProperties jHipsterProperties) {
        this.rememberMeServices = rememberMeServices;
        this.jHipsterProperties = jHipsterProperties;
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
                    .contentSecurityPolicy(csp -> csp.policyDirectives(jHipsterProperties.getSecurity().getContentSecurityPolicy()))
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
                        request -> request.getRequestURI().equals("/management/prometheus")
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
                    .key(jHipsterProperties.getSecurity().getRememberMe().getKey())
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

    /**
     * Custom CSRF handler to provide BREACH protection.
     *
     * @see <a
     * href="https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html#csrf-integration-javascript-spa">Spring
     * Security Documentation - Integrating with CSRF Protection</a>
     * @see <a href="https://github.com/jhipster/generator-jhipster/pull/25907">JHipster - use
     * customized SpaCsrfTokenRequestHandler to handle CSRF token</a>
     * @see <a href="https://stackoverflow.com/q/74447118/65681">CSRF protection not working with
     * Spring Security 6</a>
     */
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
            /*
             * If the request contains a request header, use CsrfTokenRequestAttributeHandler
             * to resolve the CsrfToken. This applies when a single-page application includes
             * the header value automatically, which was obtained via a cookie containing the
             * raw CsrfToken.
             */
            if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
                return this.plain.resolveCsrfTokenValue(request, csrfToken);
            }
            /*
             * In all other cases (e.g. if the request contains a request parameter), use
             * XorCsrfTokenRequestAttributeHandler to resolve the CsrfToken. This applies
             * when a server-side rendered form includes the _csrf request parameter as a
             * hidden input.
             */
            return this.xor.resolveCsrfTokenValue(request, csrfToken);
        }
    }
}
