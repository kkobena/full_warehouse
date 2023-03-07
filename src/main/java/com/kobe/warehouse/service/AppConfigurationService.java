package com.kobe.warehouse.service;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.AppConfiguration;
import com.kobe.warehouse.repository.AppConfigurationRepository;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AppConfigurationService {
  private final AppConfigurationRepository appConfigurationRepository;

  public AppConfigurationService(AppConfigurationRepository appConfigurationRepository) {
    this.appConfigurationRepository = appConfigurationRepository;
  }

  @Transactional(readOnly = true)
  public boolean isMono() {
    Optional<AppConfiguration> appConfiguration =
        appConfigurationRepository.findById(EntityConstant.APP_GESTION_STOCK);
    if (appConfiguration.isEmpty()) return true;
    return Integer.valueOf(appConfiguration.get().getValue().trim()) == 0;
  }

  @Transactional(readOnly = true)
  public Optional<AppConfiguration> findOneById(String id) {
    return appConfigurationRepository.findById(id);
  }

  @Transactional(readOnly = true)
  @Cacheable(EntityConstant.APP_GESTION_STOCK)
  public Optional<AppConfiguration> findStockParam() {
    return appConfigurationRepository.findById(EntityConstant.APP_GESTION_STOCK);
  }

  @Transactional
  public void update(AppConfiguration appConfiguration) {
    appConfigurationRepository.save(appConfiguration);
  }
}
