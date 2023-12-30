package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.service.InventaireService;
import com.kobe.warehouse.service.dto.StoreInventoryDTO;
import com.kobe.warehouse.service.dto.filter.StoreInventoryExportRecord;
import com.kobe.warehouse.service.dto.filter.StoreInventoryFilterRecord;
import com.kobe.warehouse.service.dto.records.ItemsCountRecord;
import com.kobe.warehouse.service.dto.records.StoreInventoryRecord;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Optional;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/** REST controller for managing {@link com.kobe.warehouse.domain.StoreInventory}. */
@RestController
@RequestMapping("/api")
public class StoreInventoryResource {

  private static final String ENTITY_NAME = "storeInventory";
  private final Logger log = LoggerFactory.getLogger(StoreInventoryResource.class);
  private final InventaireService inventaireService;

  @Value("${jhipster.clientApp.name}")
  private String applicationName;

  public StoreInventoryResource(InventaireService inventaireService) {
    this.inventaireService = inventaireService;
  }

  @GetMapping("/store-inventories/close/{id}")
  public ResponseEntity<ItemsCountRecord> closeInventory(@PathVariable Long id) {

    return ResponseEntity.ok(inventaireService.close(id));
  }

  @GetMapping("/store-inventories")
  public ResponseEntity<List<StoreInventoryDTO>> getAllStoreInventories(
      StoreInventoryFilterRecord storeInventoryFilterRecord, Pageable pageable) {
    Page<StoreInventoryDTO> page =
        inventaireService.storeInventoryList(storeInventoryFilterRecord, pageable);
    HttpHeaders headers =
        PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(), page);
    return ResponseEntity.ok().headers(headers).body(page.getContent());
  }

  @DeleteMapping("/store-inventories/{id}")
  public ResponseEntity<Void> deleteStoreInventory(@PathVariable Long id) {
    log.debug("REST request to delete StoreInventory : {}", id);
    inventaireService.remove(id);
    return ResponseEntity.noContent()
        .headers(
            HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
        .build();
  }

  @GetMapping("/store-inventories/{id}")
  public ResponseEntity<StoreInventoryDTO> getStoreInventory(@PathVariable Long id) {
    log.debug("REST request to get storeInventoryDTO : {}", id);
    Optional<StoreInventoryDTO> storeInventoryDTO = inventaireService.getStoreInventory(id);
    return ResponseUtil.wrapOrNotFound(storeInventoryDTO);
  }

  @PostMapping("/store-inventories")
  public ResponseEntity<StoreInventoryDTO> create(
      @Valid @RequestBody StoreInventoryRecord storeInventoryRecord) {
    log.debug("REST request to save storeInventory : {}", storeInventoryRecord);
    return ResponseEntity.ok().body(inventaireService.create(storeInventoryRecord));
  }

  @GetMapping("/store-inventories/proccessing/{id}")
  public ResponseEntity<StoreInventoryDTO> getStoreInventoryProccessing(@PathVariable Long id) {
    log.debug("REST request to get storeInventoryDTO : {}", id);
    Optional<StoreInventoryDTO> storeInventoryDTO =
        inventaireService.getProccessingStoreInventory(id);
    return ResponseUtil.wrapOrNotFound(storeInventoryDTO);
  }

  @PostMapping("/store-inventories/pdf")
  public ResponseEntity<Resource> getPdf(
      @RequestBody StoreInventoryExportRecord filterRecord, HttpServletRequest request)
      throws MalformedURLException {
    Resource resource = this.inventaireService.printToPdf(filterRecord);
    return Utils.printPDF(resource, request);
  }
}
