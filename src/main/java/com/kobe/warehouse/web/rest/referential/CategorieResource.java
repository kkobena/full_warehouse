package com.kobe.warehouse.web.rest.referential;

import com.kobe.warehouse.domain.Categorie;
import com.kobe.warehouse.repository.CategorieRepository;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.Categorie}.
 */
@RestController
@RequestMapping("/api/categories")
@Transactional
public class CategorieResource {

    private static final String ENTITY_NAME = "categorie";
    private final Logger log = LoggerFactory.getLogger(CategorieResource.class);
    private final CategorieRepository categorieRepository;

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    public CategorieResource(CategorieRepository categorieRepository) {
        this.categorieRepository = categorieRepository;
    }

    /**
     * {@code POST  /categories} : Create a new categorie.
     *
     * @param categorie the categorie to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new
     * categorie, or with status {@code 400 (Bad Request)} if the categorie has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<Categorie> createCategorie(@Valid @RequestBody Categorie categorie) throws URISyntaxException {
        log.debug("REST request to save Categorie : {}", categorie);
        if (categorie.getId() != null) {
            throw new BadRequestAlertException("A new categorie cannot already have an ID", ENTITY_NAME, "idexists");
        }
        Categorie result = categorieRepository.save(categorie);
        return ResponseEntity.created(new URI("/api/categories/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /categories/:id} : Updates an existing categorie.
     *
     * @param id        the id of the categorie to save.
     * @param categorie the categorie to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated
     * categorie, or with status {@code 400 (Bad Request)} if the categorie is not valid, or with
     * status {@code 500 (Internal Server Error)} if the categorie couldn't be updated.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Categorie> updateCategorie(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody Categorie categorie
    ) {
        log.debug("REST request to update Categorie : {}, {}", id, categorie);
        if (categorie.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, categorie.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!categorieRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Categorie result = categorieRepository.save(categorie);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, categorie.getId().toString()))
            .body(result);
    }

    /**
     * {@code PATCH  /categories/:id} : Partial updates given fields of an existing categorie, field
     * will ignore if it is null
     *
     * @param id        the id of the categorie to save.
     * @param categorie the categorie to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated
     * categorie, or with status {@code 400 (Bad Request)} if the categorie is not valid, or with
     * status {@code 404 (Not Found)} if the categorie is not found, or with status
     * {@code 500 (Internal Server Error)} if the categorie couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<Categorie> partialUpdateCategorie(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody Categorie categorie
    ) throws URISyntaxException {
        log.debug("REST request to partial update Categorie partially : {}, {}", id, categorie);
        if (categorie.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, categorie.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!categorieRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<Categorie> result = categorieRepository
            .findById(categorie.getId())
            .map(existingCategorie -> {
                if (categorie.getLibelle() != null) {
                    existingCategorie.setLibelle(categorie.getLibelle());
                }

                return existingCategorie;
            })
            .map(categorieRepository::save);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, categorie.getId().toString())
        );
    }

    /**
     * {@code GET  /categories} : get all the categories.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of categories in
     * body.
     */
    @GetMapping("")
    public ResponseEntity<List<Categorie>> getAllCategories(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        log.debug("REST request to get a page of Categories");
        Page<Categorie> page = categorieRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /categories/:id} : get the "id" categorie.
     *
     * @param id the id of the categorie to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the categorie,
     * or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Categorie> getCategorie(@PathVariable("id") Long id) {
        log.debug("REST request to get Categorie : {}", id);
        Optional<Categorie> categorie = categorieRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(categorie);
    }

    /**
     * {@code DELETE  /categories/:id} : delete the "id" categorie.
     *
     * @param id the id of the categorie to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategorie(@PathVariable("id") Long id) {
        log.debug("REST request to delete Categorie : {}", id);
        categorieRepository.deleteById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
