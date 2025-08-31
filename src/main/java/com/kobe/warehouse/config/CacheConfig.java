package com.kobe.warehouse.config;


import org.ehcache.config.builders.*;
import org.ehcache.jsr107.Eh107Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import java.time.Duration;

@Configuration
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        // Obtenir le provider JSR-107
        CachingProvider  cachingProvider = Caching.getCachingProvider();
        CacheManager cacheManager = cachingProvider.getCacheManager();

        // Exemple de configuration d’un cache nommé "defaultCache"
        javax.cache.configuration.Configuration<Object, Object> cacheConfig =
            Eh107Configuration.fromEhcacheCacheConfiguration(
                CacheConfigurationBuilder.newCacheConfigurationBuilder(
                        Object.class, Object.class,
                        ResourcePoolsBuilder.heap(1000)  // nombre d’objets en mémoire
                    ).withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMinutes(10)))
                    .build()
            );

        cacheManager.createCache("defaultCache", cacheConfig);
        return cacheManager;
    }
}
