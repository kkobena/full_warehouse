package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.service.InventaireService;
import com.kobe.warehouse.service.dto.StoreInventoryLineDTO;
import com.kobe.warehouse.service.dto.filter.StoreInventoryLineFilterRecord;
import com.kobe.warehouse.service.dto.records.StoreInventoryLineRecord;
import com.kobe.warehouse.web.rest.errors.BadRequestAlertException;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/** REST controller for managing {@link com.kobe.warehouse.domain.StoreInventoryLine}. */
@RestController
@RequestMapping("/api")
public class StoreInventoryLineResource {

  private static final String ENTITY_NAME = "storeInventoryLine";
  private final Logger log = LoggerFactory.getLogger(StoreInventoryLineResource.class);
  private final InventaireService inventaireService;

  public StoreInventoryLineResource(InventaireService inventaireService) {
    this.inventaireService = inventaireService;
  }

  @PutMapping("/store-inventory-lines")
  public ResponseEntity<StoreInventoryLineRecord> updateStoreInventoryLine(
      @Valid @RequestBody StoreInventoryLineDTO storeInventoryLine) {
    log.debug("REST request to update StoreInventoryLine : {}", storeInventoryLine);
    if (storeInventoryLine.getProduitId() == null) {
      throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "produitId null");
    }
    return ResponseUtil.wrapOrNotFound(
        Optional.ofNullable(inventaireService.updateQuantityOnHand(storeInventoryLine)));
  }

  @GetMapping("/store-inventory-lines")
  public ResponseEntity<List<StoreInventoryLineRecord>> getAllByInventory(
      StoreInventoryLineFilterRecord storeInventoryLineFilterRecord, Pageable pageable) {
    Page<StoreInventoryLineRecord> page =
        this.inventaireService.getAllByInventory(storeInventoryLineFilterRecord, pageable);
    HttpHeaders headers =
        PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(), page);
    return ResponseEntity.ok().headers(headers).body(page.getContent());
  }

  @GetMapping("/store-inventory-lines/items")
  public ResponseEntity<List<StoreInventoryLineRecord>> getInventoryItems(
      StoreInventoryLineFilterRecord storeInventoryLineFilterRecord, Pageable pageable) {
    Page<StoreInventoryLineRecord> page =
        this.inventaireService.getInventoryItems(storeInventoryLineFilterRecord, pageable);
    HttpHeaders headers =
        PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(), page);
    return ResponseEntity.ok().headers(headers).body(page.getContent());
  }
}
