package com.kobe.warehouse.config;

import com.kobe.warehouse.aop.license.LicenseEnforcementAspect;
import com.kobe.warehouse.aop.license.LicenseEnforcementInterceptor;
import com.kobe.warehouse.service.license.LicenseService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Active le blocage en écriture (exigence B4) uniquement lorsque le contrôle de licence est activé.
 *
 * <p>Le conditionnement porte sur les <em>beans d'enforcement</em>, sur le modèle de
 * {@link LoggingAspectConfiguration} : en profil {@code dev} et dans les tests
 * ({@code pharma-smart.license.enabled=false}), ni l'aspect ni l'intercepteur ne sont créés — la
 * chaîne d'exécution reste strictement identique à ce qu'elle était avant l'introduction de la
 * licence, ce qui garantit la non-régression de la CI.
 */
@Configuration
@ConditionalOnProperty(prefix = "pharma-smart.license", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LicenseEnforcementConfiguration implements WebMvcConfigurer {

    private final LicenseService licenseService;

    public LicenseEnforcementConfiguration(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @Bean
    public LicenseEnforcementAspect licenseEnforcementAspect() {
        return new LicenseEnforcementAspect(licenseService);
    }

    @Bean
    public LicenseEnforcementInterceptor licenseEnforcementInterceptor() {
        return new LicenseEnforcementInterceptor(licenseService);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(licenseEnforcementInterceptor());
    }
}
