package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.service.FamilleProduitService;
import com.kobe.warehouse.service.dto.FamilleProduitDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.web.rest.errors.BadRequestAlertException;

import javax.validation.Valid;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.FamilleProduit}.
 */
@RestController
@RequestMapping("/api")
public class FamilleProduitResource {

    private static final String ENTITY_NAME = "familleProduit";
    private final Logger log = LoggerFactory.getLogger(FamilleProduitResource.class);
    private final FamilleProduitService familleProduitService;
    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    public FamilleProduitResource(FamilleProduitService familleProduitService) {
        this.familleProduitService = familleProduitService;
    }

    /**
     * {@code POST  /famille-produits} : Create a new familleProduit.
     *
     * @param familleProduitDTO the familleProduitDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new familleProduitDTO, or with status {@code 400 (Bad Request)} if the familleProduit has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/famille-produits")
    public ResponseEntity<FamilleProduitDTO> createFamilleProduit(@Valid @RequestBody FamilleProduitDTO familleProduitDTO)
        throws URISyntaxException {
        log.debug("REST request to save FamilleProduit : {}", familleProduitDTO);
        if (familleProduitDTO.getId() != null) {
            throw new BadRequestAlertException("A new familleProduit cannot already have an ID", ENTITY_NAME, "idexists");
        }
        FamilleProduitDTO result = familleProduitService.save(familleProduitDTO);
        return ResponseEntity
            .created(new URI("/api/famille-produits/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /famille-produits} : Updates an existing familleProduit.
     *
     * @param familleProduitDTO the familleProduitDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated familleProduitDTO,
     * or with status {@code 400 (Bad Request)} if the familleProduitDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the familleProduitDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/famille-produits")
    public ResponseEntity<FamilleProduitDTO> updateFamilleProduit(@Valid @RequestBody FamilleProduitDTO familleProduitDTO)
        throws URISyntaxException {
        log.debug("REST request to update FamilleProduit : {}", familleProduitDTO);
        if (familleProduitDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        FamilleProduitDTO result = familleProduitService.save(familleProduitDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, familleProduitDTO.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET  /famille-produits} : get all the familleProduits.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of familleProduits in body.
     */
    @GetMapping(value = "/famille-produits")
    public ResponseEntity<List<FamilleProduitDTO>> getAllFamilleProduits(
        @RequestParam(value = "search", required = false, defaultValue = "") String search,
        Pageable pageable
    ) {
        log.debug("REST request to get a page of FamilleProduits");
        Page<FamilleProduitDTO> page = familleProduitService.findAll(search, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /famille-produits/:id} : get the "id" familleProduit.
     *
     * @param id the id of the familleProduitDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the familleProduitDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/famille-produits/{id}")
    public ResponseEntity<FamilleProduitDTO> getFamilleProduit(@PathVariable Long id) {
        log.debug("REST request to get FamilleProduit : {}", id);
        Optional<FamilleProduitDTO> familleProduitDTO = familleProduitService.findOne(id);
        return ResponseUtil.wrapOrNotFound(familleProduitDTO);
    }

    /**
     * {@code DELETE  /famille-produits/:id} : delete the "id" familleProduit.
     *
     * @param id the id of the familleProduitDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/famille-produits/{id}")
    public ResponseEntity<Void> deleteFamilleProduit(@PathVariable Long id) {
        log.debug("REST request to delete FamilleProduit : {}", id);
        familleProduitService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @PostMapping("/famille-produits/importcsv")
    public ResponseEntity<ResponseDTO> uploadFile(@RequestPart("importcsv") MultipartFile file) throws URISyntaxException, IOException {
        ResponseDTO responseDTO = familleProduitService.importation(file.getInputStream());
        return ResponseUtil.wrapOrNotFound(Optional.of(responseDTO));
    }
}
