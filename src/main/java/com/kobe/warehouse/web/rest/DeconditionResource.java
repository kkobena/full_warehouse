package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.domain.Decondition;
import com.kobe.warehouse.service.DeconditionService;
import com.kobe.warehouse.service.dto.DeconditionDTO;
import com.kobe.warehouse.web.rest.errors.BadRequestAlertException;

import com.kobe.warehouse.web.rest.errors.StockException;
import io.github.jhipster.web.util.HeaderUtil;
import io.github.jhipster.web.util.PaginationUtil;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.Decondition}.
 */
@RestController
@RequestMapping("/api")
public class DeconditionResource {

    private final Logger log = LoggerFactory.getLogger(DeconditionResource.class);

    private static final String ENTITY_NAME = "decondition";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final DeconditionService deconditionService;

    public DeconditionResource(DeconditionService deconditionService) {
        this.deconditionService = deconditionService;
    }

    /**
     * {@code POST  /deconditions} : Create a new decondition.
     *
     * @param decondition the decondition to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new decondition, or with status {@code 400 (Bad Request)} if the decondition has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/deconditions")
    public ResponseEntity<?> createDecondition(@Valid @RequestBody DeconditionDTO decondition) throws Exception {
        log.debug("REST request to save Decondition : {}", decondition);
        deconditionService.save(decondition);
        return ResponseEntity.ok().build();

    }



    /**
     * {@code GET  /deconditions} : get all the deconditions.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of deconditions in body.
     */
    @GetMapping("/deconditions")
    public ResponseEntity<List<Decondition>> getAllDeconditions(Pageable pageable) {
        log.debug("REST request to get a page of Deconditions");
        Page<Decondition> page = deconditionService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }



}
