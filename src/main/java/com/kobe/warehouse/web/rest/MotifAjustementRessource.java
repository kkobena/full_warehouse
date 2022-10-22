package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.domain.MotifAjustement;
import com.kobe.warehouse.repository.MotifAjustementRepository;

import com.kobe.warehouse.service.dto.FournisseurDTO;
import com.kobe.warehouse.web.rest.errors.BadRequestAlertException;
import io.github.jhipster.web.util.HeaderUtil;
import io.github.jhipster.web.util.PaginationUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api")
public class MotifAjustementRessource {
private final MotifAjustementRepository  motifAjustementRepository;
    private static final String ENTITY_NAME = "motifAjustement";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;
    public MotifAjustementRessource(MotifAjustementRepository motifAjustementRepository) {
        this.motifAjustementRepository = motifAjustementRepository;
    }

    @PostMapping("/motif-ajsutements")
    public ResponseEntity<Void> create(@Valid @RequestBody MotifAjustement entity) throws Exception {

        if (entity.getId() != null) {
            throw new BadRequestAlertException("A new MotifAjustement cannot already have an ID", ENTITY_NAME, "idexists");
        }
        motifAjustementRepository.save(entity);
        return ResponseEntity.ok().build();
    }
    @PutMapping("/motif-ajsutements")
    public ResponseEntity<Void> update(@Valid @RequestBody MotifAjustement entity) throws  Exception {

        if (entity.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        motifAjustementRepository.save(entity);
        return ResponseEntity.ok()
            .build();
    }
    @DeleteMapping("/motif-ajsutements/{id}")
    public ResponseEntity<Void> deleter(@PathVariable Long id) throws  Exception {
        motifAjustementRepository.deleteById(id);
        return ResponseEntity.noContent().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build();
    }

    @GetMapping(value = "/motif-ajsutements")
    public ResponseEntity<List<MotifAjustement>> getAll(@RequestParam(value = "search",required = false) String search, Pageable pageable) {
        Page<MotifAjustement> page = motifAjustementRepository.findAllByLibelleContainingIgnoreCaseOrderByLibelleAsc(search,pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }
}
