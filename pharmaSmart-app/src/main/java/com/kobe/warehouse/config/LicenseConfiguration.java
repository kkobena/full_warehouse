package com.kobe.warehouse.config;

import com.kobe.warehouse.license.HardwareFingerprintProvider;
import com.kobe.warehouse.license.LicenseProperties;
import com.kobe.warehouse.license.LicenseVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Beans du socle de licence.
 *
 * <p>Ils sont déclarés <em>inconditionnellement</em>, même lorsque
 * {@code pharma-smart.license.enabled=false} : c'est {@code LicenseService} qui court-circuite
 * l'évaluation dans ce cas. Rendre les beans eux-mêmes conditionnels obligerait chaque
 * collaborateur à les injecter en {@code Optional}, pour un gain nul — ces objets sont sans état
 * coûteux et leur seule construction ne touche ni la base ni le réseau.
 */
@Configuration
public class LicenseConfiguration {

    @Bean
    public LicenseVerifier licenseVerifier(LicenseProperties properties) {
        return LicenseVerifier.fromClasspath(properties.getPublicKeys());
    }

    @Bean
    public HardwareFingerprintProvider hardwareFingerprintProvider() {
        return new HardwareFingerprintProvider();
    }
}
