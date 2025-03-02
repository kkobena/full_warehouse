package com.kobe.warehouse.web.rest.sales;

import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.service.errors.StockException;
import com.kobe.warehouse.service.sale.SaleService;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.jhipster.web.util.HeaderUtil;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.SalesLine}.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class SalesLineResource {

    private static final String ENTITY_NAME = "salesLine";
    private final Logger log = LoggerFactory.getLogger(SalesLineResource.class);
    private final SaleService saleService;
    private final SalesLineRepository salesLineRepository;

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    public SalesLineResource(SaleService saleService, SalesLineRepository salesLineRepository) {
        this.saleService = saleService;
        this.salesLineRepository = salesLineRepository;
    }

    @GetMapping("/sales-lines/{id}")
    public ResponseEntity<List<SaleLineDTO>> getAllSalesLines(@PathVariable Long id) {
        log.debug("REST request to get a page of SalesLines");
        List<SaleLineDTO> salesLines = salesLineRepository
            .findBySalesIdOrderByProduitLibelle(id)
            .stream()
            .map(SaleLineDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok().body(salesLines);
    }

    @PostMapping("/sales-lines")
    public ResponseEntity<Sales> createSaleLine(@Valid @RequestBody SaleLineDTO saleLine) throws URISyntaxException, StockException {
        log.debug("REST request to save saleLine : {}", saleLine);
        if (saleLine.getId() != null) {
            throw new BadRequestAlertException("A new sales cannot already have an ID", ENTITY_NAME, "idexists");
        }
        Sales result = null; // = saleService.createSaleLine(saleLine);
        return ResponseEntity.created(new URI("/api/sales-line/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PutMapping("/sales-lines")
    public ResponseEntity<SaleLineDTO> updateSaleLine(@Valid @RequestBody SaleLineDTO saleLine) {
        log.debug("REST request to update saleLine : {}", saleLine);
        if (saleLine.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        SaleLineDTO result = saleService.updateSaleLine(saleLine);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, saleLine.getId().toString()))
            .body(result);
    }

    @DeleteMapping("/sales-lines/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.debug("REST request to delete sales-lines : {}", id);
        saleService.deleteSaleLineById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
