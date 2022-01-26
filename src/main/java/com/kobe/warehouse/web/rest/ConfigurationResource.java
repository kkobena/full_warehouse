package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.AppConfiguration;
import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.repository.AjustService;
import com.kobe.warehouse.service.AjustementService;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.dto.AjustDTO;
import com.kobe.warehouse.service.dto.AjustementDTO;
import io.github.jhipster.web.util.HeaderUtil;
import io.github.jhipster.web.util.PaginationUtil;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.Ajustement}.
 */
@RestController
@RequestMapping("/api")
public class ConfigurationResource {
    @Value("${jhipster.clientApp.name}")
    private String applicationName;
    private final Logger log = LoggerFactory.getLogger(ConfigurationResource.class);
    private final AppConfigurationService appConfigurationService;

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
        Optional<AppConfiguration> appConfiguration = appConfigurationService.findOneById(EntityConstant.APP_GESTION_STOCK);
        return ResponseUtil.wrapOrNotFound(appConfiguration);
    }


}
