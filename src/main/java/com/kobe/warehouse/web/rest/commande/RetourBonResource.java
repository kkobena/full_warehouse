package com.kobe.warehouse.web.rest.commande;

import com.kobe.warehouse.domain.enumeration.RetourStatut;
import com.kobe.warehouse.service.RetourBonService;
import com.kobe.warehouse.service.dto.RetourBonDTO;
import com.kobe.warehouse.web.util.HeaderUtil;
import com.kobe.warehouse.web.util.PaginationUtil;
import com.kobe.warehouse.web.util.ResponseUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.RetourBon}.
 */
@RestController
@RequestMapping("/api")
public class RetourBonResource {

    private final Logger log = LoggerFactory.getLogger(RetourBonResource.class);

    private static final String ENTITY_NAME = "retourBon";

    @Value("${pharma-smart.clientApp.name}")
    private String applicationName;

    private final RetourBonService retourBonService;

    public RetourBonResource(RetourBonService retourBonService) {
        this.retourBonService = retourBonService;
    }

    /**
     * {@code POST  /retour-bons} : Create a new retour bon.
     *
     * @param retourBonDTO the retourBonDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new retourBonDTO,
     * or with status {@code 400 (Bad Request)} if the retourBon has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/retour-bons")
    public ResponseEntity<RetourBonDTO> createRetourBon(@Valid @RequestBody RetourBonDTO retourBonDTO)
        throws URISyntaxException {
        log.debug("REST request to save RetourBon : {}", retourBonDTO);
        if (retourBonDTO.getId() != null) {
            return ResponseEntity.badRequest()
                .headers(HeaderUtil.createFailureAlert(applicationName, true, ENTITY_NAME, "idexists", "A new retour bon cannot already have an ID"))
                .body(null);
        }
        RetourBonDTO result = retourBonService.create(retourBonDTO);
        return ResponseEntity
            .created(new URI("/api/retour-bons/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /retour-bons} : Updates an existing retour bon.
     *
     * @param retourBonDTO the retourBonDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated retourBonDTO,
     * or with status {@code 400 (Bad Request)} if the retourBonDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the retourBonDTO couldn't be updated.
     */
    @PutMapping("/retour-bons")
    public ResponseEntity<RetourBonDTO> updateRetourBon(@Valid @RequestBody RetourBonDTO retourBonDTO) {
        log.debug("REST request to update RetourBon : {}", retourBonDTO);
        if (retourBonDTO.getId() == null) {
            return ResponseEntity.badRequest()
                .headers(HeaderUtil.createFailureAlert(applicationName, true, ENTITY_NAME, "idnull", "Invalid id"))
                .body(null);
        }
        RetourBonDTO result = retourBonService.update(retourBonDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, retourBonDTO.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET  /retour-bons} : get all the retour bons.
     *
     * @param pageable the pagination information.
     * @param statut   filter by status.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of retour bons in body.
     */
    @GetMapping("/retour-bons")
    public ResponseEntity<List<RetourBonDTO>> getAllRetourBons(
        @RequestParam(required = false) RetourStatut statut,
        Pageable pageable
    ) {
        log.debug("REST request to get a page of RetourBons");
        Page<RetourBonDTO> page;
        if (statut != null) {
            page = retourBonService.findAllByStatut(statut, pageable);
        } else {
            page = retourBonService.findAll(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /retour-bons/:id} : get the "id" retour bon.
     *
     * @param id the id of the retourBonDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the retourBonDTO,
     * or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/retour-bons/{id}")
    public ResponseEntity<RetourBonDTO> getRetourBon(@PathVariable Long id) {
        log.debug("REST request to get RetourBon : {}", id);
        Optional<RetourBonDTO> retourBonDTO = retourBonService.findOne(id);
        return ResponseUtil.wrapOrNotFound(retourBonDTO);
    }

    /**
     * {@code GET  /retour-bons/by-commande/:commandeId/:orderDate} : get retour bons by commande.
     *
     * @param commandeId the commande id.
     * @param orderDate  the order date.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the list of retour bons.
     */
    @GetMapping("/retour-bons/by-commande/{commandeId}/{orderDate}")
    public ResponseEntity<List<RetourBonDTO>> getRetourBonsByCommande(
        @PathVariable Long commandeId,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate orderDate
    ) {
        log.debug("REST request to get RetourBons by commande : {}, {}", commandeId, orderDate);
        List<RetourBonDTO> result = retourBonService.findAllByCommande(commandeId, orderDate);
        return ResponseEntity.ok().body(result);
    }

    /**
     * {@code GET  /retour-bons/by-date-range} : get retour bons by date range.
     *
     * @param startDate the start date.
     * @param endDate   the end date.
     * @param pageable  the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the list of retour bons.
     */
    @GetMapping("/retour-bons/by-date-range")
    public ResponseEntity<List<RetourBonDTO>> getRetourBonsByDateRange(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
        Pageable pageable
    ) {
        log.debug("REST request to get RetourBons by date range : {} - {}", startDate, endDate);
        Page<RetourBonDTO> page = retourBonService.findAllByDateRange(startDate, endDate, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code DELETE  /retour-bons/:id} : delete the "id" retour bon.
     *
     * @param id the id of the retourBonDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/retour-bons/{id}")
    public ResponseEntity<Void> deleteRetourBon(@PathVariable Long id) {
        log.debug("REST request to delete RetourBon : {}", id);
        retourBonService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code PUT  /retour-bons/:id/validate} : validate the "id" retour bon.
     *
     * @param id the id of the retourBonDTO to validate.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated retourBonDTO.
     */
    @PutMapping("/retour-bons/{id}/validate")
    public ResponseEntity<RetourBonDTO> validateRetourBon(@PathVariable Long id) {
        log.debug("REST request to validate RetourBon : {}", id);
        RetourBonDTO result = retourBonService.validate(id);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .body(result);
    }
}
