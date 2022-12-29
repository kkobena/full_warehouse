package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.domain.AppConfiguration;
import com.kobe.warehouse.service.AppConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.jhipster.web.util.ResponseUtil;

import java.util.Optional;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.Ajustement}.
 */
@RestController
@RequestMapping("/api")
public class ConfigurationResource {

    private final Logger log = LoggerFactory.getLogger(ConfigurationResource.class);
    private final AppConfigurationService appConfigurationService;
    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    public ConfigurationResource(AppConfigurationService appConfigurationService) {
        this.appConfigurationService = appConfigurationService;
    }

    @GetMapping("/app/{id}")
    public ResponseEntity<AppConfiguration> getParam(@PathVariable String id) {
        log.debug("REST request to get AppConfiguration : {}", id);
        Optional<AppConfiguration> appConfiguration = appConfigurationService.findOneById(id);
        return ResponseUtil.wrapOrNotFound(appConfiguration);
    }

    @GetMapping("/app/param-gestion-stock")
    public ResponseEntity<AppConfiguration> getParamGestionStock() {
        Optional<AppConfiguration> appConfiguration = appConfigurationService.findStockParam();
        return ResponseUtil.wrapOrNotFound(appConfiguration);
    }
}
