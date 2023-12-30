package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.repository.MagasinRepository;
import com.kobe.warehouse.service.dto.MagasinDTO;
import com.kobe.warehouse.web.rest.errors.BadRequestAlertException;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** REST controller for managing {@link com.kobe.warehouse.domain.Magasin}. */
@RestController
@RequestMapping("/api")
@Transactional
public class MagasinResource {

  private static final String ENTITY_NAME = "magasin";
  private final Logger log = LoggerFactory.getLogger(MagasinResource.class);
  private final MagasinRepository magasinRepository;

  @Value("${jhipster.clientApp.name}")
  private String applicationName;

  public MagasinResource(MagasinRepository magasinRepository) {
    this.magasinRepository = magasinRepository;
  }

  /**
   * {@code POST /magasins} : Create a new magasin.
   *
   * @param magasin the magasin to create.
   * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new
   *     magasin, or with status {@code 400 (Bad Request)} if the magasin has already an ID.
   * @throws URISyntaxException if the Location URI syntax is incorrect.
   */
  @PostMapping("/magasins")
  public ResponseEntity<Magasin> createMagasin(@Valid @RequestBody Magasin magasin)
      throws URISyntaxException {
    log.debug("REST request to save Magasin : {}", magasin);
    if (magasin.getId() != null) {
      throw new BadRequestAlertException(
          "A new magasin cannot already have an ID", ENTITY_NAME, "idexists");
    }
    Magasin result = magasinRepository.save(magasin);
    return ResponseEntity.created(new URI("/api/magasins/" + result.getId()))
        .headers(
            HeaderUtil.createEntityCreationAlert(
                applicationName, true, ENTITY_NAME, result.getId().toString()))
        .body(result);
  }

  /**
   * {@code PUT /magasins} : Updates an existing magasin.
   *
   * @param magasin the magasin to update.
   * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated
   *     magasin, or with status {@code 400 (Bad Request)} if the magasin is not valid, or with
   *     status {@code 500 (Internal Server Error)} if the magasin couldn't be updated.
   * @throws URISyntaxException if the Location URI syntax is incorrect.
   */
  @PutMapping("/magasins")
  public ResponseEntity<Magasin> updateMagasin(@Valid @RequestBody Magasin magasin)
      throws URISyntaxException {
    log.debug("REST request to update Magasin : {}", magasin);
    if (magasin.getId() == null) {
      throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
    }
    Magasin result = magasinRepository.save(magasin);
    return ResponseEntity.ok()
        .headers(
            HeaderUtil.createEntityUpdateAlert(
                applicationName, true, ENTITY_NAME, magasin.getId().toString()))
        .body(result);
  }

  /**
   * {@code GET /magasins} : get all the magasins.
   *
   * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of magasins in
   *     body.
   */
  @GetMapping("/magasins")
  public ResponseEntity<MagasinDTO> getAllMagasins() {
    log.debug("REST request to get all Magasins");
    Optional<MagasinDTO> magasin;
    List<MagasinDTO> magasins =
        magasinRepository.findAll().stream().map(MagasinDTO::new).collect(Collectors.toList());
    if (!magasins.isEmpty()) {
      magasin = Optional.of(magasins.get(0));
      return ResponseUtil.wrapOrNotFound(magasin);
    }
    return ResponseUtil.wrapOrNotFound(Optional.empty());
  }

  /**
   * {@code GET /magasins/:id} : get the "id" magasin.
   *
   * @param id the id of the magasin to retrieve.
   * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the magasin, or
   *     with status {@code 404 (Not Found)}.
   */
  @GetMapping("/magasins/{id}")
  public ResponseEntity<MagasinDTO> getMagasin(@PathVariable Long id) {
    log.debug("REST request to get Magasin : {}", id);
    Optional<MagasinDTO> magasin = magasinRepository.findById(id).map(MagasinDTO::new);
    return ResponseUtil.wrapOrNotFound(magasin);
  }

  /**
   * {@code DELETE /magasins/:id} : delete the "id" magasin.
   *
   * @param id the id of the magasin to delete.
   * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
   */
  @DeleteMapping("/magasins/{id}")
  public ResponseEntity<Void> deleteMagasin(@PathVariable Long id) {
    log.debug("REST request to delete Magasin : {}", id);
    magasinRepository.deleteById(id);
    return ResponseEntity.noContent()
        .headers(
            HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
        .build();
  }
}
