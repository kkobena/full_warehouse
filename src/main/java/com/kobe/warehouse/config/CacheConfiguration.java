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
        manager.setCaches(
            Arrays.asList(
                buildCache(EntityConstant.CURRENT_USER_CACHE, 24, TimeUnit.HOURS, 100),
                buildCache(EntityConstant.APP_MONO_STOCK, 24, TimeUnit.HOURS, 100),
                buildCache(EntityConstant.USER_STORAGE_CACHE, 24, TimeUnit.HOURS, 100),
                buildCache(EntityConstant.USER_RESERVE_STORAGE_CACHE, 24, TimeUnit.HOURS, 100),
                buildCache(EntityConstant.USER_MAIN_STORAGE_CACHE, 24, TimeUnit.HOURS, 100),
                buildCache(EntityConstant.CURRENT_USER_MAGASIN_CACHE, 24, TimeUnit.HOURS, 100),
                buildCache(EntityConstant.EXCLUDE_FREE_UNIT, 24, TimeUnit.HOURS, 100),
                buildCache(EntityConstant.APP_NOMBRE_JOUR_AVANT_PEREMPTION, 24, TimeUnit.HOURS, 100),
                buildCache(EntityConstant.APP_EXPIRY_ALERT_DAYS_BEFORE, 24, TimeUnit.HOURS, 100),
                buildCache(EntityConstant.USER_MAGASIN, 24, TimeUnit.HOURS, 100),
                buildCache(EntityConstant.APP_POS_PRINTER_ITEM_COUNT_PER_PAGE, 24, TimeUnit.HOURS, 100),
                buildCache(EntityConstant.APP_POS_PRINTER_MARGIN, 24, TimeUnit.HOURS, 100),
                buildCache(EntityConstant.APP_POS_PRINTER_WIDTH, 24, TimeUnit.HOURS, 100),
                buildCache(EntityConstant.APP_SUGGESTION_RETENTION, 24, TimeUnit.HOURS, 100),
                buildCache(EntityConstant.APP_RESET_INVOICE_NUMBER, 24, TimeUnit.HOURS, 100),
                buildCache(EntityConstant.APP_GESTION_LOT, 24, TimeUnit.HOURS, 100),
                buildCache(EntityConstant.APP_GESTION_STOCK, 24, TimeUnit.HOURS, 100),
                buildCache(EntityConstant.DEFAULT_MAIN_STORAGE, 24, TimeUnit.HOURS, 100),
                buildCache(EntityConstant.APP_MODE_PAYMENTS, 24, TimeUnit.HOURS, 100),
                buildCache(EntityConstant.APP_NBRE_JOUR_RETENTION_COMMANDE, 24, TimeUnit.HOURS, 100),
                buildCache(EntityConstant.APP_NBRE_JOUR_RETENTION_SUGGESTION, 24, TimeUnit.HOURS, 100),
                buildCache(EntityConstant.POINT_DE_VENTE_CACHE, 24, TimeUnit.HOURS, 100)
            )
        );
        return manager;
    }

    private CaffeineCache buildCache(String name, int ttl, TimeUnit timeUnit, int maxSize) {
        return new CaffeineCache(name, Caffeine.newBuilder().expireAfterWrite(ttl, timeUnit).maximumSize(maxSize).build());
    }
}
