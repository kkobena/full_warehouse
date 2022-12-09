package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.repository.AjustService;
import com.kobe.warehouse.service.AjustementService;
import com.kobe.warehouse.service.dto.AjustDTO;
import com.kobe.warehouse.service.dto.AjustementDTO;
import com.kobe.warehouse.service.dto.FournisseurDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.Ajustement}.
 */
@RestController
@RequestMapping("/api")
public class AjustementResource {

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final Logger log = LoggerFactory.getLogger(AjustementResource.class);
    private final AjustService ajustService;
    private final AjustementService ajustementService;

    public AjustementResource(AjustService ajustService, AjustementService ajustementService) {
        this.ajustService = ajustService;
        this.ajustementService = ajustementService;
    }

    @GetMapping("/ajustements")
    public List<AjustementDTO> getAllAjustements(@RequestParam(required = false, name = "id") Long id) {
        log.debug("REST request to get all Ajustements");
        if (id == null) {
            return ajustementService.findAll().stream().map(AjustementDTO::new).collect(Collectors.toList());
        }
        return ajustementService.findAll(id).stream().map(AjustementDTO::new).collect(Collectors.toList());
    }

    @PostMapping("/ajustements")
    public ResponseEntity<AjustementDTO> createAjustementDTO(@Valid @RequestBody AjustementDTO ajustementDTO) {
        log.debug("REST request to save ajustementDTO : {}", ajustementDTO);
        ajustementDTO = ajustementService.save(ajustementDTO);
        return ResponseEntity.ok().body(ajustementDTO);
    }

    @PutMapping("/ajustements")
    public ResponseEntity<AjustementDTO> updateAjustementDTO(@Valid @RequestBody AjustementDTO ajustementDTO) {
        log.debug("REST request to save ajustementDTO : {}", ajustementDTO);
        ajustementDTO = ajustementService.update(ajustementDTO);
        return ResponseEntity.ok().body(ajustementDTO);
    }

    @PostMapping("/ajustements/save")
    public ResponseEntity<Void> saveAjustementDTO(@Valid @RequestBody AjustementDTO ajustementDTO) {
        log.debug("REST request to save ajustementDTO : {}", ajustementDTO);
        ajustementService.saveAjust(ajustementDTO);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/ajustements/ajust")
    public ResponseEntity<List<AjustDTO>> getAllAjustts(
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(name = "fromDate", required = false, defaultValue = "2020-01-01") LocalDate fromDate,
        @RequestParam(name = "toDate", required = false, defaultValue = "2030-01-01") LocalDate toDate,
        Pageable pageable
    ) {
        Page<AjustDTO> page = ajustService.loadAll(search, fromDate, toDate, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @DeleteMapping("/ajustements/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ajustementService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, "ajustement", id.toString()))
            .build();
    }
}
