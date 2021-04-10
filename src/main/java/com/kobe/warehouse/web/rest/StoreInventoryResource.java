package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.domain.Reference;
import com.kobe.warehouse.service.InventaireService;
import com.kobe.warehouse.service.dto.StoreInventoryDTO;
import io.github.jhipster.web.util.HeaderUtil;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.StoreInventory}.
 */
@RestController
@RequestMapping("/api")
public class StoreInventoryResource {

    private final Logger log = LoggerFactory.getLogger(StoreInventoryResource.class);

    private static final String ENTITY_NAME = "storeInventory";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final InventaireService inventaireService;

    public StoreInventoryResource(InventaireService inventaireService) {
        this.inventaireService = inventaireService;
    }


    @GetMapping("/store-inventories/init")
    public ResponseEntity<Void> createStoreInventory() throws Exception {
        inventaireService.init();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/store-inventories/close/{id}")
    public ResponseEntity<Void> closeInventory(@PathVariable Long id) throws Exception {
        inventaireService.close(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/store-inventories")
    public ResponseEntity<List<StoreInventoryDTO>> getAllStoreInventories() {
        List<StoreInventoryDTO> data = inventaireService.storeInventoryList();
        return ResponseEntity.ok().body(data);
    }

    @DeleteMapping("/store-inventories/{id}")
    public ResponseEntity<Void> deleteStoreInventory(@PathVariable Long id) {
        log.debug("REST request to delete StoreInventory : {}", id);
        inventaireService.remove(id);
        return ResponseEntity.noContent().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build();
    }

    @GetMapping("/store-inventories/{id}")
    public ResponseEntity<StoreInventoryDTO> getStoreInventory(@PathVariable Long id) {
        log.debug("REST request to get storeInventoryDTO : {}", id);
        Optional<StoreInventoryDTO> storeInventoryDTO = inventaireService.getStoreInventory(id);
        return ResponseUtil.wrapOrNotFound(storeInventoryDTO);
    }
}
