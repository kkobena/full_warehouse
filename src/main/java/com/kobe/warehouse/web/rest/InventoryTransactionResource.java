package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.domain.InventoryTransaction;
import com.kobe.warehouse.service.InventoryTransactionService;
import com.kobe.warehouse.service.dto.InventoryTransactionDTO;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.InventoryTransaction}.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class InventoryTransactionResource {

    private final Logger log = LoggerFactory.getLogger(InventoryTransactionResource.class);
    private final InventoryTransactionService inventoryTransactionService;

    public InventoryTransactionResource(InventoryTransactionService inventoryTransactionService) {
        this.inventoryTransactionService = inventoryTransactionService;
    }

    @GetMapping("/inventory-transactions")
    public ResponseEntity<List<InventoryTransactionDTO>> getAllInventoryTransactions(
        @RequestParam(name = "produitId", required = false) Long produitId,
        @RequestParam(name = "endDate", required = false) String endDate,
        @RequestParam(name = "startDate", required = false) String startDate,
        @RequestParam(name = "type", required = false) Integer type
    ) {
        return ResponseEntity.ok().body(inventoryTransactionService.getAllInventoryTransactions(produitId, startDate, endDate, type));
    }

    @GetMapping("/inventory-transactions/{id}")
    public ResponseEntity<InventoryTransaction> getInventoryTransaction(@PathVariable Long id) {
        log.debug("REST request to get InventoryTransaction : {}", id);
        Optional<InventoryTransaction> inventoryTransaction = inventoryTransactionService.findById(id);
        return ResponseUtil.wrapOrNotFound(inventoryTransaction);
    }
}
