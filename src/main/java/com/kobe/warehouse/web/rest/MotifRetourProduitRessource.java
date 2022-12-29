package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.domain.MotifRetourProduit;
import com.kobe.warehouse.repository.MotifRetourProduitRepository;
import com.kobe.warehouse.web.rest.errors.BadRequestAlertException;

import javax.validation.Valid;

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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MotifRetourProduitRessource {

    private static final String ENTITY_NAME = "motifRetourProduit";
    private final MotifRetourProduitRepository motifRetourProduitRepository;
    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    public MotifRetourProduitRessource(MotifRetourProduitRepository motifRetourProduitRepository) {
        this.motifRetourProduitRepository = motifRetourProduitRepository;
    }

    @PostMapping("/motif-retour-produits")
    public ResponseEntity<Void> create(@Valid @RequestBody MotifRetourProduit entity) throws Exception {
        if (entity.getId() != null) {
            throw new BadRequestAlertException("A new MotifRetourProduit cannot already have an ID", ENTITY_NAME, "idexists");
        }
        motifRetourProduitRepository.save(entity);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/motif-retour-produits")
    public ResponseEntity<Void> update(@Valid @RequestBody MotifRetourProduit entity) throws Exception {
        if (entity.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        motifRetourProduitRepository.save(entity);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/motif-retour-produits/{id}")
    public ResponseEntity<Void> deleter(@PathVariable Long id) throws Exception {
        motifRetourProduitRepository.deleteById(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @GetMapping(value = "/motif-retour-produits")
    public ResponseEntity<List<MotifRetourProduit>> getAll(
        @RequestParam(value = "search", required = false) String search,
        Pageable pageable
    ) {
        Page<MotifRetourProduit> page = motifRetourProduitRepository.findAllByLibelleContainingIgnoreCaseOrderByLibelleAsc(
            search,
            pageable
        );
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }
}
