package com.kobe.warehouse.web.rest.referential;

import com.kobe.warehouse.domain.MotifAjustement;
import com.kobe.warehouse.repository.MotifAjustementRepository;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.web.util.HeaderUtil;
import com.kobe.warehouse.web.util.PaginationUtil;
import jakarta.validation.Valid;
import java.util.List;
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

@RestController
@RequestMapping("/api")
public class MotifAjustementRessource {

    private static final String ENTITY_NAME = "motifAjustement";
    private final MotifAjustementRepository motifAjustementRepository;

    @Value("${pharma-smart.clientApp.name}")
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
    public ResponseEntity<Void> update(@Valid @RequestBody MotifAjustement entity) throws Exception {
        if (entity.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        motifAjustementRepository.save(entity);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/motif-ajsutements/{id}")
    public ResponseEntity<Void> deleter(@PathVariable Long id) throws Exception {
        motifAjustementRepository.deleteById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @GetMapping(value = "/motif-ajsutements")
    public ResponseEntity<List<MotifAjustement>> getAll(
        @RequestParam(value = "search", required = false) String search,
        Pageable pageable
    ) {
        Page<MotifAjustement> page = motifAjustementRepository.findAllByLibelleContainingIgnoreCaseOrderByLibelleAsc(search, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }
}
