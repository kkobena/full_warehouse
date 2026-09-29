package com.kobe.warehouse.config;

import com.kobe.warehouse.security.navaccess.NavAccessInterceptor;
import com.kobe.warehouse.security.navaccess.NavAccessMode;
import com.kobe.warehouse.security.navaccess.NavAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Contrôle des endpoints par les droits des menus. Le mode ({@code AUDIT}, {@code ENFORCE},
 * {@code OFF}) se change par la propriété {@code pharma-smart.security.nav-access.mode}, sans
 * redéploiement de code (docs/PLAN-SECURISATION-ENDPOINTS.md § 7).
 */
@Configuration
public class NavAccessConfiguration implements WebMvcConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(NavAccessConfiguration.class);

    private final NavAccessService navAccessService;
    private final NavAccessMode mode;

    public NavAccessConfiguration(
        NavAccessService navAccessService,
        @Value("${pharma-smart.security.nav-access.mode:ENFORCE}") NavAccessMode mode
    ) {
        this.navAccessService = navAccessService;
        this.mode = mode;
        LOG.info("Contrôle des droits sur les endpoints : mode {}", mode);
    }

    @Bean
    public NavAccessInterceptor navAccessInterceptor() {
        return new NavAccessInterceptor(navAccessService, mode);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(navAccessInterceptor()).addPathPatterns("/api/**");
    }
}
