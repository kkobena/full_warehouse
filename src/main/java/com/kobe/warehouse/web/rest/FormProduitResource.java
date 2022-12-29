package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.service.FormProduitService;
import com.kobe.warehouse.service.dto.FormProduitDTO;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.FormProduit}.
 */
@RestController
@RequestMapping("/api")
public class FormProduitResource {

    private static final String ENTITY_NAME = "formProduit";
    private final Logger log = LoggerFactory.getLogger(FormProduitResource.class);
    private final FormProduitService formProduitService;
    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    public FormProduitResource(FormProduitService formProduitService) {
        this.formProduitService = formProduitService;
    }

    /**
     * {@code POST  /form-produits} : Create a new formProduit.
     *
     * @param formProduitDTO the formProduitDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new formProduitDTO, or with status {@code 400 (Bad Request)} if the formProduit has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/form-produits")
    public ResponseEntity<FormProduitDTO> createFormProduit(@Valid @RequestBody FormProduitDTO formProduitDTO) throws URISyntaxException {
        log.debug("REST request to save FormProduit : {}", formProduitDTO);
        if (formProduitDTO.getId() != null) {
            throw new BadRequestAlertException("A new formProduit cannot already have an ID", ENTITY_NAME, "idexists");
        }
        FormProduitDTO result = formProduitService.save(formProduitDTO);
        return ResponseEntity
            .created(new URI("/api/form-produits/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /form-produits} : Updates an existing formProduit.
     *
     * @param formProduitDTO the formProduitDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated formProduitDTO,
     * or with status {@code 400 (Bad Request)} if the formProduitDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the formProduitDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/form-produits")
    public ResponseEntity<FormProduitDTO> updateFormProduit(@Valid @RequestBody FormProduitDTO formProduitDTO) throws URISyntaxException {
        log.debug("REST request to update FormProduit : {}", formProduitDTO);
        if (formProduitDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        FormProduitDTO result = formProduitService.save(formProduitDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, formProduitDTO.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET  /form-produits} : get all the formProduits.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of formProduits in body.
     */
    @GetMapping("/form-produits")
    public ResponseEntity<List<FormProduitDTO>> getAllFormProduits(Pageable pageable) {
        log.debug("REST request to get a page of FormProduits");
        Page<FormProduitDTO> page = formProduitService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /form-produits/:id} : get the "id" formProduit.
     *
     * @param id the id of the formProduitDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the formProduitDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/form-produits/{id}")
    public ResponseEntity<FormProduitDTO> getFormProduit(@PathVariable Long id) {
        log.debug("REST request to get FormProduit : {}", id);
        Optional<FormProduitDTO> formProduitDTO = formProduitService.findOne(id);
        return ResponseUtil.wrapOrNotFound(formProduitDTO);
    }

    /**
     * {@code DELETE  /form-produits/:id} : delete the "id" formProduit.
     *
     * @param id the id of the formProduitDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/form-produits/{id}")
    public ResponseEntity<Void> deleteFormProduit(@PathVariable Long id) {
        log.debug("REST request to delete FormProduit : {}", id);

        formProduitService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
