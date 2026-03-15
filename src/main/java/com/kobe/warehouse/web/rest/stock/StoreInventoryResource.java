package com.kobe.warehouse.web.rest.stock;

import com.kobe.warehouse.service.InventaireService;
import com.kobe.warehouse.service.dto.StoreInventoryDTO;
import com.kobe.warehouse.service.dto.filter.StoreInventoryExportRecord;
import com.kobe.warehouse.service.dto.filter.StoreInventoryFilterRecord;
import com.kobe.warehouse.service.dto.records.ImportResultRecord;
import com.kobe.warehouse.service.dto.records.InventoryProgressRecord;
import com.kobe.warehouse.service.dto.records.ItemsCountRecord;
import com.kobe.warehouse.service.dto.records.StoreInventoryRecord;
import com.kobe.warehouse.service.stock.InventaireCreationService;
import com.kobe.warehouse.service.stock.InventaireImportService;
import com.kobe.warehouse.service.stock.InventaireProgressService;
import com.kobe.warehouse.service.stock.InventoryCloseService;
import com.kobe.warehouse.web.util.HeaderUtil;
import com.kobe.warehouse.web.util.PaginationUtil;
import com.kobe.warehouse.web.util.ResponseUtil;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.StoreInventory}.
 */
@RestController
@RequestMapping("/api")
public class StoreInventoryResource {

    private static final String ENTITY_NAME = "storeInventory";
    private final Logger log = LoggerFactory.getLogger(StoreInventoryResource.class);
    private final InventaireService inventaireService;
    private final InventaireCreationService inventaireCreationService;
    private final InventaireProgressService inventaireProgressService;
    private final InventaireImportService inventaireImportService;
    private final InventoryCloseService inventoryCloseService;

    @Value("${pharma-smart.clientApp.name}")
    private String applicationName;

    public StoreInventoryResource(
        InventaireService inventaireService,
        InventaireCreationService inventaireCreationService,
        InventaireProgressService inventaireProgressService,
        InventaireImportService inventaireImportService,
        InventoryCloseService inventoryCloseService
    ) {
        this.inventaireService = inventaireService;
        this.inventaireCreationService = inventaireCreationService;
        this.inventaireProgressService = inventaireProgressService;
        this.inventaireImportService = inventaireImportService;
        this.inventoryCloseService = inventoryCloseService;
    }

    @GetMapping("/store-inventories/close/{id}")
    public ResponseEntity<ItemsCountRecord> closeInventory(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryCloseService.close(id));
    }

    @GetMapping("/store-inventories")
    public ResponseEntity<List<StoreInventoryDTO>> getAllStoreInventories(
        StoreInventoryFilterRecord storeInventoryFilterRecord,
        Pageable pageable
    ) {
        Page<StoreInventoryDTO> page = inventaireService.storeInventoryList(
            storeInventoryFilterRecord, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @DeleteMapping("/store-inventories/{id}")
    public ResponseEntity<Void> deleteStoreInventory(@PathVariable Long id) {
        log.debug("REST request to delete StoreInventory : {}", id);
        inventaireService.remove(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME,
                id.toString()))
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
        return ResponseEntity.ok().body(inventaireCreationService.create(storeInventoryRecord));
    }

    @GetMapping("/store-inventories/proccessing/{id}")
    public ResponseEntity<StoreInventoryDTO> getStoreInventoryProccessing(@PathVariable Long id) {
        log.debug("REST request to get storeInventoryDTO : {}", id);
        Optional<StoreInventoryDTO> storeInventoryDTO = inventaireService.getProccessingStoreInventory(
            id);
        return ResponseUtil.wrapOrNotFound(storeInventoryDTO);
    }

    @PostMapping(value = "/store-inventories/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getPdf(@RequestBody StoreInventoryExportRecord filterRecord) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=inventaire.pdf");
        return ResponseEntity.ok().headers(headers)
            .body(inventaireService.printToPdf(filterRecord));
    }

    @GetMapping("/store-inventories/{id}/progress")
    public ResponseEntity<InventoryProgressRecord> getProgress(@PathVariable Long id) {
        log.debug("REST request to get inventory progress : {}", id);
        return ResponseEntity.ok(inventaireProgressService.getProgress(id));
    }

    @PostMapping("/store-inventories/{id}/import")
    public ResponseEntity<ImportResultRecord> importCsv(
        @PathVariable Long id,
        @RequestParam("file") MultipartFile file
    ) {
        log.debug("REST request to import CSV for inventory : {}", id);
        return ResponseEntity.ok(inventaireImportService.importDetail(id, file));
    }
}
