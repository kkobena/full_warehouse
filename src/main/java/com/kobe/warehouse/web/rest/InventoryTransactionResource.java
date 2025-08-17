package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.domain.InventoryTransaction;
import com.kobe.warehouse.service.dto.InventoryTransactionDTO;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.web.util.PaginationUtil;
import com.kobe.warehouse.web.util.ResponseUtil;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** REST controller for managing {@link com.kobe.warehouse.domain.InventoryTransaction}. */
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
        @RequestParam(name = "type", required = false) Integer type,
        Pageable pageable
    ) {
        Page<InventoryTransactionDTO> page = inventoryTransactionService.getAllInventoryTransactions(
            pageable,
            produitId,
            startDate,
            endDate,
            type
        );
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/inventory-transactions/{id}")
    public ResponseEntity<InventoryTransaction> getInventoryTransaction(@PathVariable Long id) {
        log.debug("REST request to get InventoryTransaction : {}", id);
        Optional<InventoryTransaction> inventoryTransaction = inventoryTransactionService.findById(id);
        return ResponseUtil.wrapOrNotFound(inventoryTransaction);
    }
}
