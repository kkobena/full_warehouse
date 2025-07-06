package com.kobe.warehouse.service;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.AppConfiguration;
import com.kobe.warehouse.domain.Magasin;
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
        return true;/*
        Optional<AppConfiguration> appConfiguration = appConfigurationRepository.findById(EntityConstant.APP_GESTION_STOCK);
        return appConfiguration.map(configuration -> Integer.parseInt(configuration.getValue().trim()) == 0).orElse(true);*/
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

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_GESTION_LOT)
    public Optional<Boolean> useLot() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_GESTION_LOT)
            .map(configuration -> {
                try {
                    return Integer.parseInt(configuration.getValue().trim()) == 1;
                } catch (NumberFormatException e) {
                    return false;
                }
            });
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

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_RESET_INVOICE_NUMBER)
    public Optional<AppConfiguration> findParamResetInvoiceNumberEveryYear() {
        return appConfigurationRepository.findById(EntityConstant.APP_RESET_INVOICE_NUMBER);
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_SUGGESTION_RETENTION)
    public int findSuggestionRetention() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_SUGGESTION_RETENTION)
            .map(appConfiguration -> {
                try {
                    return Integer.parseInt(appConfiguration.getValue());
                } catch (NumberFormatException _) {
                    return 90;
                }
            })
            .orElse(90);
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_POS_PRINTER_WIDTH)
    public int getPrinterWidth() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_POS_PRINTER_WIDTH)
            .map(AppConfiguration::getValue)
            .map(Integer::parseInt)
            .orElse(576);
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_POS_PRINTER_MARGIN)
    public int getPrinterMargin() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_POS_PRINTER_MARGIN)
            .map(AppConfiguration::getValue)
            .map(Integer::parseInt)
            .orElse(9);
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_POS_PRINTER_ITEM_COUNT_PER_PAGE)
    public int getPrinterItemCount() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_POS_PRINTER_ITEM_COUNT_PER_PAGE)
            .map(AppConfiguration::getValue)
            .map(Integer::parseInt)
            .orElse(40);
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.USER_MAGASIN)
    public Magasin getMagasin() {
        return userService.getUser().getMagasin();
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_EXPIRY_ALERT_DAYS_BEFORE)
    public int getNombreJourAlertPeremption() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_EXPIRY_ALERT_DAYS_BEFORE)
            .map(AppConfiguration::getValue)
            .map(Integer::parseInt)
            .orElse(30);
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_NOMBRE_JOUR_AVANT_PEREMPTION)
    public int getNombreJourPeremption() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_NOMBRE_JOUR_AVANT_PEREMPTION)
            .map(AppConfiguration::getValue)
            .map(Integer::parseInt)
            .orElse(7);
    }
}
