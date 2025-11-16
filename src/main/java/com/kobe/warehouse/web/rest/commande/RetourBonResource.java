package com.kobe.warehouse.web.rest.commande;

import com.kobe.warehouse.domain.enumeration.RetourStatut;
import com.kobe.warehouse.service.dto.ReponseRetourBonDTO;
import com.kobe.warehouse.service.dto.RetourBonDTO;
import com.kobe.warehouse.service.stock.RetourBonService;
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
import org.springframework.format.annotation.DateTimeFormat;
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
 * REST controller for managing {@link com.kobe.warehouse.domain.RetourBon}.
 */
@RestController
@RequestMapping("/api")
public class RetourBonResource {

    private static final String ENTITY_NAME = "retourBon";
    private final Logger log = LoggerFactory.getLogger(RetourBonResource.class);
    private final RetourBonService retourBonService;

    @Value("${pharma-smart.clientApp.name}")
    private String applicationName;

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
    public ResponseEntity<RetourBonDTO> createRetourBon(@Valid @RequestBody RetourBonDTO retourBonDTO) throws URISyntaxException {
        log.debug("REST request to save RetourBon : {}", retourBonDTO);
        if (retourBonDTO.getId() != null) {
            return ResponseEntity.badRequest()
                .headers(
                    HeaderUtil.createFailureAlert(
                        applicationName,
                        true,
                        ENTITY_NAME,
                        "idexists",
                        "A new retour bon cannot already have an ID"
                    )
                )
                .body(null);
        }
        RetourBonDTO result = retourBonService.create(retourBonDTO);
        return ResponseEntity.created(new URI("/api/retour-bons/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
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
        @RequestParam(required = false, name = "") RetourStatut statut,
        @RequestParam(required = false, name = "dtStart") LocalDate dtStart,
        @RequestParam(required = false, name = "dtEnd") LocalDate dtEnd,
        Pageable pageable
    ) {
        log.debug("REST request to get a page of RetourBons");
        Page<RetourBonDTO> page = retourBonService.findAll(pageable);

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
    public ResponseEntity<RetourBonDTO> getRetourBon(@PathVariable Integer id) {
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
        @PathVariable Integer commandeId,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate orderDate
    ) {
        log.debug("REST request to get RetourBons by commande : {}, {}", commandeId, orderDate);
        List<RetourBonDTO> result = retourBonService.findAllByCommande(commandeId, orderDate);
        return ResponseEntity.ok().body(result);
    }

    /**
     * {@code POST  /retour-bons/supplier-response} : Create a supplier response for a retour bon.
     *
     * @param reponseRetourBonDTO the supplier response to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new reponseRetourBonDTO,
     * or with status {@code 400 (Bad Request)} if the reponse has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/retour-bons/supplier-response")
    public ResponseEntity<ReponseRetourBonDTO> createSupplierResponse(@Valid @RequestBody ReponseRetourBonDTO reponseRetourBonDTO)
        throws URISyntaxException {
        log.debug("REST request to create supplier response for RetourBon : {}", reponseRetourBonDTO.getRetourBonId());
        if (reponseRetourBonDTO.getId() != null) {
            return ResponseEntity.badRequest()
                .headers(
                    HeaderUtil.createFailureAlert(
                        applicationName,
                        true,
                        "reponseRetourBon",
                        "idexists",
                        "A new supplier response cannot already have an ID"
                    )
                )
                .body(null);
        }
        ReponseRetourBonDTO result = retourBonService.createSupplierResponse(reponseRetourBonDTO);
        return ResponseEntity.created(new URI("/api/retour-bons/supplier-response/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, "reponseRetourBon", result.getId().toString()))
            .body(result);
    }
}
