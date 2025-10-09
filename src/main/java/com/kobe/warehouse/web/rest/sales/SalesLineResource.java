package com.kobe.warehouse.web.rest.sales;

import com.kobe.warehouse.domain.SaleLineId;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.service.sale.SaleService;
import com.kobe.warehouse.web.util.HeaderUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SalesLineResource {

    private static final String ENTITY_NAME = "salesLine";
    private final Logger log = LoggerFactory.getLogger(SalesLineResource.class);
    private final SaleService saleService;

    @Value("${pharma-smart.clientApp.name}")
    private String applicationName;

    public SalesLineResource(SaleService saleService) {
        this.saleService = saleService;
    }

    @GetMapping("/sales-lines/{id}/{saleDate}")
    public ResponseEntity<List<SaleLineDTO>> getAllSalesLines(@PathVariable Long id, @PathVariable LocalDate saleDate) {
        return ResponseEntity.ok().body(saleService.findBySalesIdAndSalesSaleDateOrderByProduitLibelle(id, saleDate));
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

    @DeleteMapping("/sales-lines/{id}/{saleDate}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id, @PathVariable("saleDate") LocalDate saleDate) {
        log.debug("REST request to delete sales-lines : {}", id);
        saleService.deleteSaleLineById(new SaleLineId(id, saleDate));
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
