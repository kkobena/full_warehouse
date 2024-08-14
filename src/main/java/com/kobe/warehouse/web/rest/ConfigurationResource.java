package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.domain.AppConfiguration;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.web.rest.proxy.ConfigurationResourceProxy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.Ajustement}.
 */
@RestController
@RequestMapping("/api")
public class ConfigurationResource extends ConfigurationResourceProxy {

    public ConfigurationResource(AppConfigurationService appConfigurationService) {
        super(appConfigurationService);
    }

    @GetMapping("/app/{id}")
    public ResponseEntity<AppConfiguration> getParam(@PathVariable String id) {
        return super.getParam(id);
    }

    @GetMapping("/app/param-gestion-stock")
    public ResponseEntity<AppConfiguration> getParamGestionStock() {
        return super.getParamGestionStock();
    }
}
