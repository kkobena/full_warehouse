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
                buildCache(EntityConstant.APP_DELAI_REGLEMENT_FACTURE, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
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
                buildCache(EntityConstant.APP_SEUIL_VARIATION_PRIX_CACHE, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_RECEPTION_MIN_EXPIRY_DAYS_CACHE, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_PUTAWAY_MODE_CACHE, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_ACCEPTATION_SUBSTITUTION_CACHE, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_DELAI_RETOUR_FOURNISSEUR_CACHE, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_AP_DEFAULT_CREDIT_DAYS_CACHE, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_AP_DEFAULT_CRITIQUE_DAYS_CACHE, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.USER_MAGASIN, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_POS_PRINTER_ITEM_COUNT_PER_PAGE, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_SUGGESTION_RETENTION, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_RESET_INVOICE_NUMBER, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_GESTION_LOT, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_GESTION_LOT_INVENTAIRE_CACHE, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_MODE_SAISIE_LOT_INVENTAIRE_CACHE, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_GESTION_STOCK, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.DEFAULT_MAIN_STORAGE, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_MODE_PAYMENTS, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_MODE_PAYMENTS_SANS_CH_VIR, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_NBRE_JOUR_RETENTION_COMMANDE, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_NTH_MOIS_CONSOMMATION_CACHE, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_COUVERTURE_MOIS_CLASSIQUE_CACHE, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_CANCEL_SALE_MAX_DAYS_CACHE, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_NOTIF_AVOIR_EMAIL_ENABLED_CACHE, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_NOTIF_AVOIR_SMS_ENABLED_CACHE, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_DELAI_RETOUR_CLIENT_CACHE, defaultTtl, TimeUnit.HOURS, defaultMaxSize),
                buildCache(EntityConstant.APP_DELAI_VALIDITE_AVOIR_CACHE, defaultTtl, TimeUnit.HOURS, defaultMaxSize),

                buildCache("dailySalesReport", 15, TimeUnit.MINUTES, 100),
                buildCache("produits", 15, TimeUnit.MINUTES, 100),
                buildCache("dashboardAlertCounts", 5, TimeUnit.MINUTES, 100),
                buildCache("dashboardCA", 15, TimeUnit.MINUTES, 100),
                buildCache("profitability", 15, TimeUnit.MINUTES, 100),
                buildCache("margeReport", 30, TimeUnit.MINUTES, 100),
                buildCache("comparativeReports", 15, TimeUnit.MINUTES, 100),
                buildCache("marketBasketCache", 15, TimeUnit.MINUTES, 100),
                buildCache("stockAlerts", 30, TimeUnit.MINUTES, 100),
                buildCache("topProducts", 30, TimeUnit.MINUTES, 100),
                buildCache("abcPareto", 30, TimeUnit.MINUTES, 100),
                buildCache("salesForecast", 24, TimeUnit.HOURS, 50),
                buildCache("cashRegisterReport", 15, TimeUnit.MINUTES, 50),
                buildCache("tiersPayantCreances", 60, TimeUnit.MINUTES, 100),
                buildCache("stockValuation", 60, TimeUnit.MINUTES, 100),
                buildCache("supplierPerformance", 60, TimeUnit.MINUTES, 100),
                buildCache("stockValuationSummary", 60, TimeUnit.MINUTES, 10),
                buildCache("stockRotation", 60, TimeUnit.MINUTES, 100),
                buildCache("customerSegmentation", 120, TimeUnit.MINUTES, 200),
                buildCache("pnlAnalytique", 30, TimeUnit.MINUTES, 20),
                buildCache("vieillissementCreances", 15, TimeUnit.MINUTES, 10),
                buildCache("concentrationPayers", 30, TimeUnit.MINUTES, 10),
                buildCache("cashFlowBfr", 15, TimeUnit.MINUTES, 5),
                buildCache(EntityConstant.NAV_TREE_CACHE, defaultTtl, TimeUnit.HOURS, 500),
                // Layout dashboard résolu par utilisateur — 1 entrée/utilisateur, changement rare
                buildCache(EntityConstant.DASHBOARD_LAYOUT_RESOLVED_CACHE, defaultTtl, TimeUnit.HOURS, 500)
            )
        );
        return manager;
    }

    private CaffeineCache buildCache(String name, int ttl, TimeUnit timeUnit, int maxSize) {
        return new CaffeineCache(name, Caffeine.newBuilder().expireAfterWrite(ttl, timeUnit).maximumSize(maxSize).build());
    }
}
