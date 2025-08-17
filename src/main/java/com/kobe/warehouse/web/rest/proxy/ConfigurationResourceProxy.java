package com.kobe.warehouse.web.rest.proxy;

import com.kobe.warehouse.domain.AppConfiguration;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.web.util.ResponseUtil;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.Ajustement}.
 */
public class ConfigurationResourceProxy {

    private final Logger log = LoggerFactory.getLogger(ConfigurationResourceProxy.class);
    private final AppConfigurationService appConfigurationService;

    public ConfigurationResourceProxy(AppConfigurationService appConfigurationService) {
        this.appConfigurationService = appConfigurationService;
    }

    public ResponseEntity<AppConfiguration> getParam(String id) {
        log.debug("REST request to get AppConfiguration : {}", id);
        Optional<AppConfiguration> appConfiguration = appConfigurationService.findOneById(id);
        return ResponseUtil.wrapOrNotFound(appConfiguration);
    }

    public ResponseEntity<AppConfiguration> getParamGestionStock() {
        Optional<AppConfiguration> appConfiguration = appConfigurationService.findStockParam();
        return ResponseUtil.wrapOrNotFound(appConfiguration);
    }

    @GetMapping("/app")
    public ResponseEntity<List<AppConfiguration>> fetchAll(
        @RequestParam(value = "search", required = false, defaultValue = "") String search
    ) {
        return ResponseEntity.ok().body(appConfigurationService.fetchAll(search));
    }

    @PutMapping("/app")
    public ResponseEntity<Void> update(@Valid @RequestBody AppConfiguration appConfiguration) {
        appConfigurationService.update(appConfiguration);
        return ResponseEntity.accepted().build();
    }
}
