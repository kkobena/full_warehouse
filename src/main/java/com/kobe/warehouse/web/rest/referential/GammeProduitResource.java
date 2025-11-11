package com.kobe.warehouse.web.rest.referential;

import com.kobe.warehouse.service.dto.GammeProduitDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.service.referential.GammeProduitService;
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
 * REST controller for managing {@link com.kobe.warehouse.domain.GammeProduit}.
 */
@RestController
@RequestMapping("/api")
public class GammeProduitResource {

    private static final String ENTITY_NAME = "gammeProduit";
    private final Logger log = LoggerFactory.getLogger(GammeProduitResource.class);
    private final GammeProduitService gammeProduitService;

    @Value("${pharma-smart.clientApp.name}")
    private String applicationName;

    public GammeProduitResource(GammeProduitService gammeProduitService) {
        this.gammeProduitService = gammeProduitService;
    }

    /**
     * {@code POST /gamme-produits} : Create a new gammeProduit.
     *
     * @param gammeProduitDTO the gammeProduitDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new
     * gammeProduitDTO, or with status {@code 400 (Bad Request)} if the gammeProduit has already an
     * ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/gamme-produits")
    public ResponseEntity<GammeProduitDTO> createGammeProduit(@Valid @RequestBody GammeProduitDTO gammeProduitDTO)
        throws URISyntaxException {
        log.debug("REST request to save GammeProduit : {}", gammeProduitDTO);
        if (gammeProduitDTO.getId() != null) {
            throw new BadRequestAlertException("A new gammeProduit cannot already have an ID", ENTITY_NAME, "idexists");
        }
        GammeProduitDTO result = gammeProduitService.save(gammeProduitDTO);
        return ResponseEntity.created(new URI("/api/gamme-produits/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT /gamme-produits} : Updates an existing gammeProduit.
     *
     * @param gammeProduitDTO the gammeProduitDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated
     * gammeProduitDTO, or with status {@code 400 (Bad Request)} if the gammeProduitDTO is not
     * valid, or with status {@code 500 (Internal Server Error)} if the gammeProduitDTO couldn't be
     * updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/gamme-produits")
    public ResponseEntity<GammeProduitDTO> updateGammeProduit(@Valid @RequestBody GammeProduitDTO gammeProduitDTO) {
        log.debug("REST request to update GammeProduit : {}", gammeProduitDTO);
        if (gammeProduitDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        GammeProduitDTO result = gammeProduitService.save(gammeProduitDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, gammeProduitDTO.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET /gamme-produits} : get all the gammeProduits.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of gammeProduits
     * in body.
     */
    @GetMapping(value = "/gamme-produits")
    public ResponseEntity<List<GammeProduitDTO>> getAllGammeProduits(
        @RequestParam(value = "search", required = false) String search,
        Pageable pageable
    ) {
        log.debug("REST request to get a page of GammeProduits");
        Page<GammeProduitDTO> page = gammeProduitService.findAll(search, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET /gamme-produits/:id} : get the "id" gammeProduit.
     *
     * @param id the id of the gammeProduitDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the
     * gammeProduitDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/gamme-produits/{id}")
    public ResponseEntity<GammeProduitDTO> getGammeProduit(@PathVariable Integer id) {
        log.debug("REST request to get GammeProduit : {}", id);
        Optional<GammeProduitDTO> gammeProduitDTO = gammeProduitService.findOne(id);
        return ResponseUtil.wrapOrNotFound(gammeProduitDTO);
    }

    /**
     * {@code DELETE /gamme-produits/:id} : delete the "id" gammeProduit.
     *
     * @param id the id of the gammeProduitDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/gamme-produits/{id}")
    public ResponseEntity<Void> deleteGammeProduit(@PathVariable Integer id) {
        log.debug("REST request to delete GammeProduit : {}", id);

        gammeProduitService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @PostMapping("/gamme-produits/importcsv")
    public ResponseEntity<ResponseDTO> uploadFile(@RequestPart("importcsv") MultipartFile file) throws IOException {
        ResponseDTO responseDTO = gammeProduitService.importation(file.getInputStream());
        return ResponseEntity.ok().body(responseDTO);
    }
}
