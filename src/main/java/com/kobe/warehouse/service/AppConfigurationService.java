package com.kobe.warehouse.service;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.AppConfiguration;
import com.kobe.warehouse.repository.AppConfigurationRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AppConfigurationService {
    private final AppConfigurationRepository appConfigurationRepository;


    public AppConfigurationService(AppConfigurationRepository appConfigurationRepository) {
        this.appConfigurationRepository = appConfigurationRepository;
    }

    public boolean isMono() {
        Optional<AppConfiguration> appConfiguration = appConfigurationRepository.findById(EntityConstant.APP_GESTION_STOCK);
        if (appConfiguration.isEmpty()) return true;
        return Integer.valueOf(appConfiguration.get().getValue().trim()) == 0;
    }

    public Optional<AppConfiguration> findOneById(String id) {
        return appConfigurationRepository.findById(id);
    }

    @Cacheable(EntityConstant.APP_GESTION_STOCK)
    public Optional<AppConfiguration> findStockParam() {
        return appConfigurationRepository.findById(EntityConstant.APP_GESTION_STOCK);
    }
//canForceStock param
}
