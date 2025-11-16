package com.kobe.warehouse.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.kobe.warehouse.constant.EntityConstant;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfiguration {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        int defaultTtl = 24;
        int defaultMaxSize = 5;
        manager.setCaches(
            Arrays.asList(
                buildCache(EntityConstant.APP_POST_CONFIG, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_CUSTOMER_DISPLAY, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.CURRENT_USER_CACHE, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_MONO_STOCK, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.USER_STORAGE_CACHE, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.USER_RESERVE_STORAGE_CACHE, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.USER_MAIN_STORAGE_CACHE, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.CURRENT_USER_MAGASIN_CACHE, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.EXCLUDE_FREE_UNIT, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_NOMBRE_JOUR_AVANT_PEREMPTION, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_EXPIRY_ALERT_DAYS_BEFORE, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.USER_MAGASIN, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_POS_PRINTER_ITEM_COUNT_PER_PAGE, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_SUGGESTION_RETENTION, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_RESET_INVOICE_NUMBER, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_GESTION_LOT, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_GESTION_STOCK, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.DEFAULT_MAIN_STORAGE, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_MODE_PAYMENTS, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_NBRE_JOUR_RETENTION_COMMANDE, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.POINT_DE_VENTE_CACHE, defaultTtl, TimeUnit.HOURS, defaultMaxSize)
            )
        );
        return manager;
    }

    private CaffeineCache buildCache(String name, int ttl, TimeUnit timeUnit, int maxSize) {
        return new CaffeineCache(name, Caffeine.newBuilder().expireAfterWrite(ttl, timeUnit).maximumSize(maxSize).build());
    }
}
