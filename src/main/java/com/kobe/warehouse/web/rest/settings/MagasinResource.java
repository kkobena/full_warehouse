package com.kobe.warehouse.web.rest.settings;

import com.kobe.warehouse.domain.enumeration.TypeMagasin;
import com.kobe.warehouse.service.dto.MagasinDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.service.referential.magasin.MagasinService;
import com.kobe.warehouse.web.util.HeaderUtil;
import com.kobe.warehouse.web.util.ResponseUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.Magasin}.
 */
@RestController
@RequestMapping("/api")
public class MagasinResource {

    private static final String ENTITY_NAME = "magasin";
    private final Logger log = LoggerFactory.getLogger(MagasinResource.class);
    private final MagasinService magasinService;

    @Value("${pharma-smart.clientApp.name}")
    private String applicationName;

    public MagasinResource(MagasinService magasinService) {
        this.magasinService = magasinService;
    }

    /**
     * {@code POST /magasins} : Create a new magasin.
     *
     * @param magasin the magasin to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new
     * magasin, or with status {@code 400 (Bad Request)} if the magasin has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/magasins")
    public ResponseEntity<MagasinDTO> createMagasin(@Valid @RequestBody MagasinDTO magasin) throws URISyntaxException {
        log.debug("REST request to save Magasin : {}", magasin);
        if (magasin.getId() != null) {
            throw new BadRequestAlertException("A new magasin cannot already have an ID", ENTITY_NAME, "idexists");
        }
        MagasinDTO result = magasinService.save(magasin);
        return ResponseEntity.created(new URI("/api/magasins/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT /magasins} : Updates an existing magasin.
     *
     * @param magasin the magasin to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated
     * magasin, or with status {@code 400 (Bad Request)} if the magasin is not valid, or with status
     * {@code 500 (Internal Server Error)} if the magasin couldn't be updated.
     */
    @PutMapping("/magasins")
    public ResponseEntity<MagasinDTO> updateMagasin(@Valid @RequestBody MagasinDTO magasin) {
        log.debug("REST request to update Magasin : {}", magasin);
        if (magasin.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        MagasinDTO result = magasinService.save(magasin);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, magasin.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET /magasins} : get all the magasins.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of magasins in
     * body.
     */
    @GetMapping("/magasins/current-user-magasin")
    public ResponseEntity<MagasinDTO> findOne() {
        log.debug("REST request to get all Magasins");

        return ResponseUtil.wrapOrNotFound(Optional.ofNullable(magasinService.currentUserMagasin()));
    }

    /**
     * {@code GET /magasins/:id} : get the "id" magasin.
     *
     * @param id the id of the magasin to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the magasin, or
     * with status {@code 404 (Not Found)}.
     */
    @GetMapping("/magasins/{id}")
    public ResponseEntity<MagasinDTO> getMagasin(@PathVariable Integer id) {
        log.debug("REST request to get Magasin : {}", id);

        return ResponseUtil.wrapOrNotFound(Optional.ofNullable(magasinService.findById(id)));
    }

    /**
     * {@code DELETE /magasins/:id} : delete the "id" magasin.
     *
     * @param id the id of the magasin to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/magasins/{id}")
    public ResponseEntity<Void> deleteMagasin(@PathVariable Integer id) {
        log.debug("REST request to delete Magasin : {}", id);
        magasinService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @GetMapping("/magasins")
    public ResponseEntity<List<MagasinDTO>> getAllMagasins() {
        return ResponseEntity.ok().body(magasinService.findAll(Set.of()));
    }

    @GetMapping("/magasins/depots")
    public ResponseEntity<List<MagasinDTO>> getAllDepots(
        @RequestParam(required = false, name = "types") Set<TypeMagasin> types
    ) {
        return ResponseEntity.ok().body(magasinService.findAll(CollectionUtils.isEmpty(types)?Set.of(TypeMagasin.DEPOT):types));
    }

    @GetMapping("/magasins/has-depot")
    public ResponseEntity<Boolean> hasDepot() {
        return ResponseEntity.ok().body(magasinService.hasDepot());
    }

}
