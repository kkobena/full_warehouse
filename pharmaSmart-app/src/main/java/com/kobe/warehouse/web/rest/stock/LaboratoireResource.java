package com.kobe.warehouse.web.rest.stock;

import com.kobe.warehouse.service.LaboratoireService;
import com.kobe.warehouse.service.dto.LaboratoireDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.web.util.HeaderUtil;
import com.kobe.warehouse.web.util.PaginationUtil;
import com.kobe.warehouse.web.util.ResponseUtil;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
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

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.Laboratoire}.
 */
@RestController
@RequestMapping("/api")
public class LaboratoireResource {

    private static final String ENTITY_NAME = "laboratoire";
    private final Logger log = LoggerFactory.getLogger(LaboratoireResource.class);
    private final LaboratoireService laboratoireService;

    @Value("${pharma-smart.clientApp.name}")
    private String applicationName;

    public LaboratoireResource(LaboratoireService laboratoireService) {
        this.laboratoireService = laboratoireService;
    }

    /**
     * {@code POST /laboratoires} : Create a new laboratoire.
     *
     * @param laboratoireDTO the laboratoireDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new
     * laboratoireDTO, or with status {@code 400 (Bad Request)} if the laboratoire has already an
     * ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/laboratoires")
    public ResponseEntity<LaboratoireDTO> createLaboratoire(@Valid @RequestBody LaboratoireDTO laboratoireDTO) throws URISyntaxException {
        log.debug("REST request to save Laboratoire : {}", laboratoireDTO);
        if (laboratoireDTO.getId() != null) {
            throw new BadRequestAlertException("A new laboratoire cannot already have an ID", ENTITY_NAME, "idexists");
        }
        LaboratoireDTO result = laboratoireService.save(laboratoireDTO);
        return ResponseEntity.created(new URI("/api/laboratoires/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT /laboratoires} : Updates an existing laboratoire.
     *
     * @param laboratoireDTO the laboratoireDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated
     * laboratoireDTO, or with status {@code 400 (Bad Request)} if the laboratoireDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the laboratoireDTO couldn't be
     * updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/laboratoires")
    public ResponseEntity<LaboratoireDTO> updateLaboratoire(@Valid @RequestBody LaboratoireDTO laboratoireDTO) throws URISyntaxException {
        log.debug("REST request to update Laboratoire : {}", laboratoireDTO);
        if (laboratoireDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        LaboratoireDTO result = laboratoireService.save(laboratoireDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, laboratoireDTO.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET /laboratoires} : get all the laboratoires.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of laboratoires
     * in body.
     */
    @GetMapping(value = "/laboratoires")
    public ResponseEntity<List<LaboratoireDTO>> getAllLaboratoires(
        @RequestParam(value = "search", required = false, defaultValue = "") String search,
        Pageable pageable
    ) {
        log.debug("REST request to get a page of Laboratoires");
        Page<LaboratoireDTO> page = laboratoireService.findAll(search, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET /laboratoires/:id} : get the "id" laboratoire.
     *
     * @param id the id of the laboratoireDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the
     * laboratoireDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/laboratoires/{id}")
    public ResponseEntity<LaboratoireDTO> getLaboratoire(@PathVariable Integer id) {
        log.debug("REST request to get Laboratoire : {}", id);
        Optional<LaboratoireDTO> laboratoireDTO = laboratoireService.findOne(id);
        return ResponseUtil.wrapOrNotFound(laboratoireDTO);
    }

    /**
     * {@code DELETE /laboratoires/:id} : delete the "id" laboratoire.
     *
     * @param id the id of the laboratoireDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/laboratoires/{id}")
    public ResponseEntity<Void> deleteLaboratoire(@PathVariable Integer id) {
        log.debug("REST request to delete Laboratoire : {}", id);

        laboratoireService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @PostMapping("/laboratoires/importcsv")
    public ResponseEntity<ResponseDTO> uploadFile(@RequestPart("importcsv") MultipartFile file) throws URISyntaxException, IOException {
        ResponseDTO responseDTO = laboratoireService.importation(file.getInputStream());
        return ResponseEntity.ok().body(responseDTO);
    }
}
