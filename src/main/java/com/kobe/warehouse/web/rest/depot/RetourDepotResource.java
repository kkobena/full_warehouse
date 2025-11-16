package com.kobe.warehouse.web.rest.depot;

import com.kobe.warehouse.service.dto.RetourDepotDTO;
import com.kobe.warehouse.service.stock.RetourDepotService;
import com.kobe.warehouse.web.util.HeaderUtil;
import com.kobe.warehouse.web.util.PaginationUtil;
import com.kobe.warehouse.web.util.ResponseUtil;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.RetourDepot}.
 */
@RestController
@RequestMapping("/api")
public class RetourDepotResource {

    private static final String ENTITY_NAME = "retourDepot";
    private final Logger log = LoggerFactory.getLogger(RetourDepotResource.class);
    private final RetourDepotService retourDepotService;

    @Value("${pharma-smart.clientApp.name}")
    private String applicationName;

    public RetourDepotResource(RetourDepotService retourDepotService) {
        this.retourDepotService = retourDepotService;
    }

    /**
     * {@code POST  /retour-depots} : Create a new retour depot.
     *
     * @param retourDepotDTO the retourDepotDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new retourDepotDTO,
     * or with status {@code 400 (Bad Request)} if the retourDepot has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/retour-depots")
    public ResponseEntity<RetourDepotDTO> createRetourDepot(@Valid @RequestBody RetourDepotDTO retourDepotDTO) throws URISyntaxException {
        log.debug("REST request to save RetourDepot : {}", retourDepotDTO);
        if (retourDepotDTO.getId() != null) {
            return ResponseEntity.badRequest()
                .headers(
                    HeaderUtil.createFailureAlert(
                        applicationName,
                        true,
                        ENTITY_NAME,
                        "idexists",
                        "A new retour depot cannot already have an ID"
                    )
                )
                .body(null);
        }
        RetourDepotDTO result = retourDepotService.create(retourDepotDTO);
        return ResponseEntity.created(new URI("/api/retour-depots/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET  /retour-depots/:id} : get the "id" retour depot.
     *
     * @param id the id of the retourDepotDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the retourDepotDTO,
     * or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/retour-depots/{id}")
    public ResponseEntity<RetourDepotDTO> getRetourDepot(@PathVariable Integer id) {
        log.debug("REST request to get RetourDepot : {}", id);
        Optional<RetourDepotDTO> retourDepotDTO = retourDepotService.findOne(id);
        return ResponseUtil.wrapOrNotFound(retourDepotDTO);
    }

    @GetMapping("/retour-depots")
    public ResponseEntity<List<RetourDepotDTO>> getRetourDepots(
        @RequestParam(name = "depotId", required = false) Integer depotId,
        @RequestParam(name = "dtStart", required = false) LocalDate dtStart,
        @RequestParam(name = "dtEnd", required = false) LocalDate dtEnd,
        Pageable pageable
    ) {
        Page page = retourDepotService.findAllByDateRange(depotId, dtStart, dtEnd, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }
}
