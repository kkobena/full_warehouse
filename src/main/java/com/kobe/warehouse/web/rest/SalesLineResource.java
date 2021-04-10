package com.kobe.warehouse.web.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import com.kobe.warehouse.web.rest.errors.StockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.service.SaleService;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.web.rest.errors.BadRequestAlertException;

import io.github.jhipster.web.util.HeaderUtil;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.SalesLine}.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class SalesLineResource {

	private final Logger log = LoggerFactory.getLogger(SalesLineResource.class);

	private static final String ENTITY_NAME = "salesLine";

	@Value("${jhipster.clientApp.name}")
	private String applicationName;
	@Autowired
	private SaleService saleService;
	@Autowired
	private SalesLineRepository salesLineRepository;

	@GetMapping("/sales-lines/{id}")
	public ResponseEntity<List<SaleLineDTO>> getAllSalesLines(@PathVariable Long id) {
		log.debug("REST request to get a page of SalesLines");
		List<SaleLineDTO> salesLines = salesLineRepository.findBySalesIdOrderByProduitLibelle(id).stream()
				.map(SaleLineDTO::new).collect(Collectors.toList());
		return ResponseEntity.ok().body(salesLines);
	}

	@PostMapping("/sales-lines")
	public ResponseEntity<Sales> createSaleLine(@Valid @RequestBody SaleLineDTO saleLine) throws URISyntaxException, StockException {
		log.debug("REST request to save saleLine : {}", saleLine);
		if (saleLine.getId() != null) {
			throw new BadRequestAlertException("A new sales cannot already have an ID", ENTITY_NAME, "idexists");
		}
		Sales result = saleService.createSaleLine(saleLine);
		return ResponseEntity
				.created(new URI("/api/sales-line/" + result.getId())).headers(HeaderUtil
						.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
				.body(result);
	}

	@PutMapping("/sales-lines")
	public ResponseEntity<SaleLineDTO> updateSaleLine(@Valid @RequestBody SaleLineDTO saleLine) throws URISyntaxException {
		log.debug("REST request to update saleLine : {}", saleLine);
		if (saleLine.getId() == null) {
			throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
		}
		SaleLineDTO result = saleService.updateSaleLine(saleLine);
		return ResponseEntity.ok().headers(
				HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, saleLine.getId().toString()))
				.body(result);
	}


    @DeleteMapping("/sales-lines/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.debug("REST request to delete sales-lines : {}", id);
        saleService.deleteSaleLineById(id);
        return ResponseEntity.noContent().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build();
    }
}
