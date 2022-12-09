package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.domain.Importation;
import com.kobe.warehouse.domain.enumeration.ImportationStatus;
import com.kobe.warehouse.domain.enumeration.ImportationType;
import com.kobe.warehouse.domain.enumeration.TiersPayantStatut;
import com.kobe.warehouse.service.ImportationTiersPayantService;
import com.kobe.warehouse.service.TiersPayantDataService;
import com.kobe.warehouse.service.TiersPayantService;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.dto.TiersPayantDto;
import com.kobe.warehouse.web.rest.errors.BadRequestAlertException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

@RestController
@RequestMapping("/api")
public class TiersPayantResource {

    private final ImportationTiersPayantService importationTiersPayantService;
    private final TiersPayantDataService tiersPayantDataService;
    private final TiersPayantService tiersPayantService;
    private static final String ENTITY_NAME = "tiers-payant";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    public TiersPayantResource(
        ImportationTiersPayantService importationTiersPayantService,
        TiersPayantDataService tiersPayantDataService,
        TiersPayantService tiersPayantService
    ) {
        this.importationTiersPayantService = importationTiersPayantService;
        this.tiersPayantDataService = tiersPayantDataService;
        this.tiersPayantService = tiersPayantService;
    }

    @PostMapping("/tiers-payants/importjson")
    public ResponseEntity<ResponseDTO> uploadFile(@RequestPart("importjson") MultipartFile file) throws URISyntaxException, IOException {
        ResponseDTO responseDTO = importationTiersPayantService.updateStocFromJSON(file.getInputStream());
        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/tiers-payants/result")
    public ResponseEntity<ResponseDTO> getCuurent() {
        Importation importation = importationTiersPayantService.current(ImportationType.TIERS_PAYANT);
        if (importation == null) {
            return ResponseUtil.wrapOrNotFound(Optional.of(new ResponseDTO().setCompleted(true)));
        }
        ResponseDTO responseDTO = new ResponseDTO();
        responseDTO.setSize(importation.getSize());
        responseDTO.setTotalSize(importation.getTotalZise());
        if (importation.getImportationStatus() != ImportationStatus.PROCESSING) {
            responseDTO.setCompleted(true);
        }

        return ResponseUtil.wrapOrNotFound(Optional.of(responseDTO));
    }

    @GetMapping(value = "/tiers-payants")
    public ResponseEntity<List<TiersPayantDto>> getAll(
        @RequestParam(name = "groupeTiersPayantId", required = false) Long groupeTiersPayantId,
        @RequestParam(value = "search", required = false, defaultValue = "") String search,
        @RequestParam(value = "type", required = false, defaultValue = "") String type,
        Pageable pageable
    ) {
        Page<TiersPayantDto> page = tiersPayantDataService.list(search, type, TiersPayantStatut.ACTIF, groupeTiersPayantId, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @PostMapping("/tiers-payants")
    public ResponseEntity<TiersPayantDto> create(@Valid @RequestBody TiersPayantDto tiersPayantDto) throws URISyntaxException {
        if (tiersPayantDto.getId() != null) {
            throw new BadRequestAlertException("A new rayon cannot already have an ID", null, "idexists");
        }
        TiersPayantDto result = tiersPayantService.createFromDto(tiersPayantDto);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/tiers-payants")
    public ResponseEntity<TiersPayantDto> update(@Valid @RequestBody TiersPayantDto tiersPayantDto) throws URISyntaxException {
        TiersPayantDto result = tiersPayantService.updateFromDto(tiersPayantDto);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/tiers-payants/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tiersPayantService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @DeleteMapping("/tiers-payants/desable/{id}")
    public ResponseEntity<Void> desable(@PathVariable Long id) {
        tiersPayantService.desable(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
