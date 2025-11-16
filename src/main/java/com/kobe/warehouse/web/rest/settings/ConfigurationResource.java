package com.kobe.warehouse.web.rest.settings;

import com.kobe.warehouse.domain.AppConfiguration;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.web.util.ResponseUtil;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ConfigurationResource {

    private final AppConfigurationService appConfigurationService;

    public ConfigurationResource(AppConfigurationService appConfigurationService) {
        this.appConfigurationService = appConfigurationService;
    }

    @GetMapping("/app/{id}")
    public ResponseEntity<AppConfiguration> getParam(@PathVariable String id) {
        Optional<AppConfiguration> appConfiguration = appConfigurationService.findOneById(id);
        return ResponseUtil.wrapOrNotFound(appConfiguration);
    }

    @GetMapping("/app/param-gestion-stock")
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
