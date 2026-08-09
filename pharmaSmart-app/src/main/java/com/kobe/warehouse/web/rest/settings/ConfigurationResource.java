package com.kobe.warehouse.web.rest.settings;

import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.settings.dto.AppConfigurationDto;
import com.kobe.warehouse.web.util.PaginationUtil;
import com.kobe.warehouse.web.util.ResponseUtil;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api")
public class ConfigurationResource {

    private final AppConfigurationService appConfigurationService;

    public ConfigurationResource(AppConfigurationService appConfigurationService) {
        this.appConfigurationService = appConfigurationService;
    }

    @GetMapping("/app/{id}")
    public ResponseEntity<AppConfigurationDto> getParam(@PathVariable String id) {
        return ResponseUtil.wrapOrNotFound(appConfigurationService.findOne(id));
    }

    @GetMapping("/app/param-gestion-stock")
    public ResponseEntity<AppConfigurationDto> getParamGestionStock() {
        return ResponseUtil.wrapOrNotFound(appConfigurationService.findStockParam());
    }

    @GetMapping("/app")
    public ResponseEntity<List<AppConfigurationDto>> fetchAll(
        @RequestParam(value = "search", required = false, defaultValue = "") String search,
        Pageable pageable
    ) {
        Page<AppConfigurationDto> page = appConfigurationService.fetchAll(search, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @PutMapping("/app")
    public ResponseEntity<Void> update(@Valid @RequestBody AppConfigurationDto appConfiguration) {
        appConfigurationService.update(appConfiguration);
        return ResponseEntity.accepted().build();
    }

    /**
     * GET /api/app/model-reappro : Récupère la configuration du modèle de réapprovisionnement
     * et les options disponibles.
     *
     * @return ResponseEntity avec le modèle actuel et les options
     */
    @GetMapping("/app/model-reappro")
    public ResponseEntity<Map<String, Object>> getModelReappro() {
        return ResponseEntity.ok(appConfigurationService.getModelReapproConfiguration());
    }

    /**
     * PUT /api/app/model-reappro : Met à jour le modèle de réapprovisionnement.
     *
     * @param model Le nouveau modèle (CLASSIQUE ou SEMOIS)
     * @return ResponseEntity avec le statut de la mise à jour
     */
    @PutMapping("/app/model-reappro")
    public ResponseEntity<Void> updateModelReappro(@RequestParam String model) {
        try {
            appConfigurationService.updateModelReappro(model);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
