package com.kobe.warehouse.web.rest.referential;

import com.kobe.warehouse.service.dto.TableauDTO;
import com.kobe.warehouse.service.referential.TableauService;
import com.kobe.warehouse.web.rest.errors.BadRequestAlertException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
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

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.Tableau}.
 */
@RestController
@RequestMapping("/api")
public class TableauResource {

    private static final String ENTITY_NAME = "tableau";
    private final Logger log = LoggerFactory.getLogger(TableauResource.class);
    private final TableauService tableauService;
    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    public TableauResource(TableauService tableauService) {
        this.tableauService = tableauService;
    }




    @PostMapping("/tableaux")
    public ResponseEntity<TableauDTO> create(@Valid @RequestBody TableauDTO tableau) throws URISyntaxException {
        log.debug("REST request to save tableaux : {}", tableau);
        if (tableau.getId() != null) {
            throw new BadRequestAlertException("A new tableau cannot already have an ID", ENTITY_NAME, "idexists");
        }
        TableauDTO result = tableauService.save(tableau);
        return ResponseEntity
            .created(new URI("/api/tableaux/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PutMapping("/tableaux")
    public ResponseEntity<TableauDTO> update(@Valid @RequestBody TableauDTO tableauDTO) throws URISyntaxException {

        if (tableauDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        TableauDTO result = tableauService.save(tableauDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, tableauDTO.getId().toString()))
            .body(result);
    }


    @GetMapping("/tableaux")
    public ResponseEntity<List<TableauDTO>> getAllTableaux() {
        return ResponseEntity.ok().body(tableauService.findAll());
    }

    @GetMapping("/tableaux/{id}")
    public ResponseEntity<TableauDTO> getOne(@PathVariable Long id) {
        return ResponseUtil.wrapOrNotFound(tableauService.findOne(id));
    }

    @DeleteMapping("/tableaux/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tableauService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
