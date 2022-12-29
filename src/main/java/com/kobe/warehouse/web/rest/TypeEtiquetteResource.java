package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.service.TypeEtiquetteService;
import com.kobe.warehouse.service.dto.TypeEtiquetteDTO;
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

@RestController
@RequestMapping("/api")
public class TypeEtiquetteResource {

    private static final String ENTITY_NAME = "typeEtiquette";
    private final Logger log = LoggerFactory.getLogger(TypeEtiquetteResource.class);
    private final TypeEtiquetteService typeEtiquetteService;
    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    public TypeEtiquetteResource(TypeEtiquetteService typeEtiquetteService) {
        this.typeEtiquetteService = typeEtiquetteService;
    }

    /**
     * {@code POST  /type-etiquettes} : Create a new typeEtiquette.
     *
     * @param typeEtiquetteDTO the typeEtiquetteDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new typeEtiquetteDTO, or with status {@code 400 (Bad Request)} if the typeEtiquette has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/type-etiquettes")
    public ResponseEntity<TypeEtiquetteDTO> createTypeEtiquette(@Valid @RequestBody TypeEtiquetteDTO typeEtiquetteDTO)
        throws URISyntaxException {
        log.debug("REST request to save TypeEtiquette : {}", typeEtiquetteDTO);
        if (typeEtiquetteDTO.getId() != null) {
            throw new BadRequestAlertException("A new typeEtiquette cannot already have an ID", ENTITY_NAME, "idexists");
        }
        TypeEtiquetteDTO result = typeEtiquetteService.save(typeEtiquetteDTO);
        return ResponseEntity
            .created(new URI("/api/type-etiquettes/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /type-etiquettes} : Updates an existing typeEtiquette.
     *
     * @param typeEtiquetteDTO the typeEtiquetteDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated typeEtiquetteDTO,
     * or with status {@code 400 (Bad Request)} if the typeEtiquetteDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the typeEtiquetteDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/type-etiquettes")
    public ResponseEntity<TypeEtiquetteDTO> updateTypeEtiquette(@Valid @RequestBody TypeEtiquetteDTO typeEtiquetteDTO)
        throws URISyntaxException {
        log.debug("REST request to update TypeEtiquette : {}", typeEtiquetteDTO);
        if (typeEtiquetteDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        TypeEtiquetteDTO result = typeEtiquetteService.save(typeEtiquetteDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, typeEtiquetteDTO.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET  /type-etiquettes} : get all the typeEtiquettes.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of typeEtiquettes in body.
     */
    @GetMapping("/type-etiquettes")
    public ResponseEntity<List<TypeEtiquetteDTO>> getAllTypeEtiquettes(Pageable pageable) {
        log.debug("REST request to get a page of TypeEtiquettes");
        Page<TypeEtiquetteDTO> page = typeEtiquetteService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /type-etiquettes/:id} : get the "id" typeEtiquette.
     *
     * @param id the id of the typeEtiquetteDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the typeEtiquetteDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/type-etiquettes/{id}")
    public ResponseEntity<TypeEtiquetteDTO> getTypeEtiquette(@PathVariable Long id) {
        log.debug("REST request to get TypeEtiquette : {}", id);
        Optional<TypeEtiquetteDTO> typeEtiquetteDTO = typeEtiquetteService.findOne(id);
        return ResponseUtil.wrapOrNotFound(typeEtiquetteDTO);
    }

    /**
     * {@code DELETE  /type-etiquettes/:id} : delete the "id" typeEtiquette.
     *
     * @param id the id of the typeEtiquetteDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/type-etiquettes/{id}")
    public ResponseEntity<Void> deleteTypeEtiquette(@PathVariable Long id) {
        log.debug("REST request to delete TypeEtiquette : {}", id);

        typeEtiquetteService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
