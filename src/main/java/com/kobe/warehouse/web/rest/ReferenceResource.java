package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.domain.Reference;
import com.kobe.warehouse.repository.ReferenceRepository;
import com.kobe.warehouse.web.rest.errors.BadRequestAlertException;

import io.github.jhipster.web.util.HeaderUtil;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.Reference}.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class ReferenceResource {

    private final Logger log = LoggerFactory.getLogger(ReferenceResource.class);

    private static final String ENTITY_NAME = "reference";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ReferenceRepository referenceRepository;

    public ReferenceResource(ReferenceRepository referenceRepository) {
        this.referenceRepository = referenceRepository;
    }

    /**
     * {@code POST  /references} : Create a new reference.
     *
     * @param reference the reference to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new reference, or with status {@code 400 (Bad Request)} if the reference has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/references")
    public ResponseEntity<Reference> createReference(@Valid @RequestBody Reference reference) throws URISyntaxException {
        log.debug("REST request to save Reference : {}", reference);
        if (reference.getId() != null) {
            throw new BadRequestAlertException("A new reference cannot already have an ID", ENTITY_NAME, "idexists");
        }
        Reference result = referenceRepository.save(reference);
        return ResponseEntity.created(new URI("/api/references/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /references} : Updates an existing reference.
     *
     * @param reference the reference to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated reference,
     * or with status {@code 400 (Bad Request)} if the reference is not valid,
     * or with status {@code 500 (Internal Server Error)} if the reference couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/references")
    public ResponseEntity<Reference> updateReference(@Valid @RequestBody Reference reference) throws URISyntaxException {
        log.debug("REST request to update Reference : {}", reference);
        if (reference.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        Reference result = referenceRepository.save(reference);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, reference.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET  /references} : get all the references.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of references in body.
     */
    @GetMapping("/references")
    public List<Reference> getAllReferences() {
        log.debug("REST request to get all References");
        return referenceRepository.findAll();
    }

    /**
     * {@code GET  /references/:id} : get the "id" reference.
     *
     * @param id the id of the reference to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the reference, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/references/{id}")
    public ResponseEntity<Reference> getReference(@PathVariable Long id) {
        log.debug("REST request to get Reference : {}", id);
        Optional<Reference> reference = referenceRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(reference);
    }

    /**
     * {@code DELETE  /references/:id} : delete the "id" reference.
     *
     * @param id the id of the reference to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/references/{id}")
    public ResponseEntity<Void> deleteReference(@PathVariable Long id) {
        log.debug("REST request to delete Reference : {}", id);
        referenceRepository.deleteById(id);
        return ResponseEntity.noContent().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build();
    }
}
