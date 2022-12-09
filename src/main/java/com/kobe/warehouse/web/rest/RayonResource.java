package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.repository.MagasinRepository;
import com.kobe.warehouse.service.RayonService;
import com.kobe.warehouse.service.dto.RayonDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.web.rest.errors.BadRequestAlertException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.Rayon}.
 */
@RestController
@RequestMapping("/api")
public class RayonResource {

    private final Logger log = LoggerFactory.getLogger(RayonResource.class);

    private static final String ENTITY_NAME = "rayon";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final RayonService rayonService;

    public RayonResource(RayonService rayonService) {
        this.rayonService = rayonService;
    }

    /**
     * {@code POST  /rayons} : Create a new rayon.
     *
     * @param rayonDTO the rayonDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new rayonDTO, or with status {@code 400 (Bad Request)} if the rayon has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/rayons")
    public ResponseEntity<RayonDTO> createRayon(@Valid @RequestBody RayonDTO rayonDTO) throws URISyntaxException {
        log.debug("REST request to save Rayon : {}", rayonDTO);
        if (rayonDTO.getId() != null) {
            throw new BadRequestAlertException("A new rayon cannot already have an ID", ENTITY_NAME, "idexists");
        }
        RayonDTO result = rayonService.save(rayonDTO);
        return ResponseEntity
            .created(new URI("/api/rayons/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /rayons} : Updates an existing rayon.
     *
     * @param rayonDTO the rayonDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated rayonDTO,
     * or with status {@code 400 (Bad Request)} if the rayonDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the rayonDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/rayons")
    public ResponseEntity<RayonDTO> updateRayon(@Valid @RequestBody RayonDTO rayonDTO) throws URISyntaxException {
        log.debug("REST request to update Rayon : {}", rayonDTO);
        if (rayonDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        RayonDTO result = rayonService.update(rayonDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, rayonDTO.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET  /rayons} : get all the rayons.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of rayons in body.
     */
    @GetMapping(value = "/rayons")
    public ResponseEntity<List<RayonDTO>> getAllRayons(
        @RequestParam(name = "storageId", required = false) Long storageId,
        @RequestParam(value = "search", required = false, defaultValue = "") String search,
        Pageable pageable
    ) {
        log.debug("REST request to get a page of Rayons");
        Page<RayonDTO> page = rayonService.findAll(storageId, search, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /rayons/:id} : get the "id" rayon.
     *
     * @param id the id of the rayonDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the rayonDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/rayons/{id}")
    public ResponseEntity<RayonDTO> getRayon(@PathVariable Long id) {
        log.debug("REST request to get Rayon : {}", id);
        Optional<RayonDTO> rayonDTO = rayonService.findOne(id);
        return ResponseUtil.wrapOrNotFound(rayonDTO);
    }

    /**
     * {@code DELETE  /rayons/:id} : delete the "id" rayon.
     *
     * @param id the id of the rayonDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/rayons/{id}")
    public ResponseEntity<Void> deleteRayon(@PathVariable Long id) {
        log.debug("REST request to delete Rayon : {}", id);
        rayonService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @PostMapping("/rayons/importcsv/{storageId}")
    public ResponseEntity<ResponseDTO> uploadFile(
        @RequestPart("importcsv") MultipartFile file,
        @PathVariable(name = "storageId") Long storageId
    ) throws URISyntaxException, IOException {
        ResponseDTO responseDTO = rayonService.importation(file.getInputStream(), storageId);
        return ResponseUtil.wrapOrNotFound(Optional.of(responseDTO));
    }

    @PostMapping("/rayons/clone/{storageId}")
    public ResponseEntity<ResponseDTO> clonerRayons(
        @PathVariable(name = "storageId", required = false) Long storageId,
        @RequestBody List<RayonDTO> rayonIds
    ) throws URISyntaxException, IOException {
        ResponseDTO responseDTO = rayonService.cloner(rayonIds, storageId);
        return ResponseUtil.wrapOrNotFound(Optional.of(responseDTO));
    }

    @PostMapping("/rayons/importcsv")
    public ResponseEntity<ResponseDTO> importRayonFromCSV(@RequestPart("importcsv") MultipartFile file)
        throws URISyntaxException, IOException {
        ResponseDTO responseDTO = rayonService.importation(file.getInputStream(), null);
        return ResponseUtil.wrapOrNotFound(Optional.of(responseDTO));
    }
}
