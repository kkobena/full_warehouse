package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.service.FournisseurProduitService;
import com.kobe.warehouse.service.dto.FournisseurProduitDTO;
import com.kobe.warehouse.web.rest.errors.BadRequestAlertException;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.jhipster.web.util.HeaderUtil;

@RestController
@RequestMapping("/api")
public class FournisseurProduitResource {

    private static final String ENTITY_NAME = "fournisseurProduit";
    private final Logger log = LoggerFactory.getLogger(FournisseurProduitService.class);
    private final FournisseurProduitService fournisseurProduitService;
    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    public FournisseurProduitResource(FournisseurProduitService fournisseurProduitService) {
        this.fournisseurProduitService = fournisseurProduitService;
    }

    @PostMapping("/fournisseur-produits")
    public ResponseEntity<FournisseurProduitDTO> create(@Valid @RequestBody FournisseurProduitDTO dto) throws Exception {
        log.debug("REST request to save FournisseurProduitDTO : {}", dto);
        if (dto.getId() != null) {
            throw new BadRequestAlertException("A new FournisseurProduit cannot already have an ID", ENTITY_NAME, "idexists");
        }
        return ResponseEntity.ok().body(fournisseurProduitService.create(dto).orElse(null));
    }

    @PutMapping("/fournisseur-produits")
    public ResponseEntity<FournisseurProduitDTO> update(@Valid @RequestBody FournisseurProduitDTO dto) throws Exception {
        log.debug("REST request to update FournisseurProduit : {}", dto);
        if (dto.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        return ResponseEntity.ok().body(fournisseurProduitService.update(dto).orElse(null));
    }

    @DeleteMapping("/fournisseur-produits/{id}")
    public ResponseEntity<Void> deleter(@PathVariable Long id) throws Exception {
        log.debug("REST request to delete FournisseurProduit : {}", id);
        fournisseurProduitService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @PutMapping("/fournisseur-produits/{id}/{checked}")
    public ResponseEntity<Void> updateDefaultFournisseur(@PathVariable("id") Long id, @PathVariable("checked") Boolean checked)
        throws Exception {
        log.debug("REST request to delete FournisseurProduit : {}", id);
        fournisseurProduitService.updateDefaultFournisseur(id, checked);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build();
    }
}
