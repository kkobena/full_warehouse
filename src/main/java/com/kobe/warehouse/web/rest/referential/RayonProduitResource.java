package com.kobe.warehouse.web.rest.referential;

import com.kobe.warehouse.service.RayonProduitService;
import com.kobe.warehouse.service.dto.RayonProduitDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.web.util.HeaderUtil;
import com.kobe.warehouse.web.util.ResponseUtil;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class RayonProduitResource {

    private static final String ENTITY_NAME = "rayonProduit";
    private final Logger log = LoggerFactory.getLogger(RayonProduitResource.class);
    private final RayonProduitService rayonProduitService;

    @Value("${pharma-smart.clientApp.name}")
    private String applicationName;

    public RayonProduitResource(RayonProduitService rayonProduitService) {
        this.rayonProduitService = rayonProduitService;
    }

    @PostMapping("/rayon-produits")
    public ResponseEntity<RayonProduitDTO> create(@Valid @RequestBody RayonProduitDTO dto) throws Exception {
        log.debug("REST request to save RayonProduitDTO : {}", dto);
        if (dto.getId() != null) {
            throw new BadRequestAlertException("A new rayonProduit cannot already have an ID", ENTITY_NAME, "idexists");
        }
        return ResponseEntity.ok().body(rayonProduitService.create(dto).orElse(null));
    }

    @PostMapping("/rayon-produits/import")
    public ResponseEntity<ResponseDTO> importCsv(
        @RequestPart("importcsv") MultipartFile file,
        @RequestParam("storageId") Integer storageId
    ) throws IOException {
        log.debug("REST request to import RayonProduit CSV for storageId : {}", storageId);
        ResponseDTO result = rayonProduitService.importFromCsv(file.getInputStream(), storageId);
        return ResponseUtil.wrapOrNotFound(Optional.of(result));
    }

    @DeleteMapping("/rayon-produits/{id}")
    public ResponseEntity<Void> deleter(@PathVariable Integer id) {
        log.debug("REST request to delete rayonProduit : {}", id);
        rayonProduitService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
