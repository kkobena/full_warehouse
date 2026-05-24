package com.kobe.warehouse.web.rest.sales;

import com.kobe.warehouse.domain.SaleLineId;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.sale.SaleService;
import com.kobe.warehouse.web.util.HeaderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

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


    @DeleteMapping("/sales-lines/{id}/{saleDate}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id, @PathVariable("saleDate") LocalDate saleDate) {
        log.debug("REST request to delete sales-lines : {}", id);
        saleService.deleteSaleLineById(new SaleLineId(id, saleDate));
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
