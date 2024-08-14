package com.kobe.warehouse.service;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.AppConfiguration;
import com.kobe.warehouse.repository.AppConfigurationRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AppConfigurationService {

    private final UserService userService;
    private final AppConfigurationRepository appConfigurationRepository;

    public AppConfigurationService(UserService userService, AppConfigurationRepository appConfigurationRepository) {
        this.userService = userService;
        this.appConfigurationRepository = appConfigurationRepository;
    }

    @Transactional(readOnly = true)
    public boolean isMono() {
        Optional<AppConfiguration> appConfiguration = appConfigurationRepository.findById(EntityConstant.APP_GESTION_STOCK);
        return appConfiguration.map(configuration -> Integer.parseInt(configuration.getValue().trim()) == 0).orElse(true);
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
        appConfigurationRepository
            .findById(appConfiguration.getName())
            .map(configuration -> {
                configuration.setValue(appConfiguration.getValue());
                configuration.setDescription(appConfiguration.getDescription());
                configuration.setUpdated(LocalDateTime.now());
                configuration.setValidatedBy(userService.getUser());
                return appConfigurationRepository.save(configuration);
            });
    }

    @Transactional(readOnly = true)
    public List<AppConfiguration> fetchAll(String search) {
        return appConfigurationRepository.findAllByNameOrDescriptionContainingAllIgnoreCase(
            search,
            search,
            Sort.by(Sort.Direction.ASC, "description")
        );
    }
}
