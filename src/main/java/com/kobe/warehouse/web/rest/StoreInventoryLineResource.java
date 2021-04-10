package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.service.InventaireService;
import com.kobe.warehouse.service.dto.StoreInventoryDTO;
import com.kobe.warehouse.service.dto.StoreInventoryLineDTO;
import com.kobe.warehouse.web.rest.errors.BadRequestAlertException;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.StoreInventoryLine}.
 */
@RestController
@RequestMapping("/api")

public class StoreInventoryLineResource {

    private final Logger log = LoggerFactory.getLogger(StoreInventoryLineResource.class);

    private static final String ENTITY_NAME = "storeInventoryLine";

    private final InventaireService inventaireService;

    public StoreInventoryLineResource(InventaireService inventaireService) {
        this.inventaireService = inventaireService;
    }

    /**
     * {@code PUT  /store-inventory-lines} : Updates an existing storeInventoryLine.
     *
     * @param storeInventoryLine the storeInventoryLine to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated storeInventoryLine,
     * or with status {@code 400 (Bad Request)} if the storeInventoryLine is not valid,
     * or with status {@code 500 (Internal Server Error)} if the storeInventoryLine couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/store-inventory-lines")
    public ResponseEntity<StoreInventoryDTO> updateStoreInventoryLine(@Valid @RequestBody StoreInventoryLineDTO storeInventoryLine)  {
        log.debug("REST request to update StoreInventoryLine : {}", storeInventoryLine);
        if (storeInventoryLine.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        inventaireService.updateQuantityOnHand(storeInventoryLine);
        Optional<StoreInventoryDTO> storeInventoryDTO = inventaireService.getStoreInventory(storeInventoryLine.getStoreInventoryId());
        return ResponseUtil.wrapOrNotFound(storeInventoryDTO);

    }

}
