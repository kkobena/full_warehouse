package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.service.TvaService;
import com.kobe.warehouse.service.dto.TvaDTO;
import com.kobe.warehouse.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class TvaResource {

    private static final String ENTITY_NAME = "tva";
    private final Logger log = LoggerFactory.getLogger(TvaResource.class);
    private final TvaService tvaService;
    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    public TvaResource(TvaService tvaService) {
        this.tvaService = tvaService;
    }

    /**
     * {@code POST  /tvas} : Create a new tva.
     *
     * @param tvaDTO the tvaDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new tvaDTO, or with status {@code 400 (Bad Request)} if the tva has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/tvas")
    public ResponseEntity<TvaDTO> createTva(@Valid @RequestBody TvaDTO tvaDTO) throws URISyntaxException {
        log.debug("REST request to save Tva : {}", tvaDTO);
        if (tvaDTO.getId() != null) {
            throw new BadRequestAlertException("A new tva cannot already have an ID", ENTITY_NAME, "idexists");
        }
        TvaDTO result = tvaService.save(tvaDTO);
        return ResponseEntity
            .created(new URI("/api/tvas/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET  /tvas} : get all the tvas.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of tvas in body.
     */
    @GetMapping("/tvas")
    public ResponseEntity<List<TvaDTO>> getAllTvas(Pageable pageable) {
        log.debug("REST request to get a page of Tvas");
        Page<TvaDTO> page = tvaService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /tvas/:id} : get the "id" tva.
     *
     * @param id the id of the tvaDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the tvaDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/tvas/{id}")
    public ResponseEntity<TvaDTO> getTva(@PathVariable Long id) {
        log.debug("REST request to get Tva : {}", id);
        Optional<TvaDTO> tvaDTO = tvaService.findOne(id);
        return ResponseUtil.wrapOrNotFound(tvaDTO);
    }

    /**
     * {@code DELETE  /tvas/:id} : delete the "id" tva.
     *
     * @param id the id of the tvaDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/tvas/{id}")
    public ResponseEntity<Void> deleteTva(@PathVariable Long id) {
        log.debug("REST request to delete Tva : {}", id);

        tvaService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
