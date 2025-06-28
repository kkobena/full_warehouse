package com.kobe.warehouse.web.rest.java_client.mobile;

import com.kobe.warehouse.service.InventaireService;
import com.kobe.warehouse.service.dto.RayonDTO;
import com.kobe.warehouse.service.dto.StoreInventoryDTO;
import com.kobe.warehouse.service.dto.StoreInventoryLineDTO;
import com.kobe.warehouse.service.mobile.dto.RayonInventaireDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/java-client/mobile/inventories")
public class MobileInventoryResource {
    private final InventaireService inventaireService;

    public MobileInventoryResource(InventaireService inventaireService) {
        this.inventaireService = inventaireService;
    }

    @GetMapping
    public ResponseEntity<List<StoreInventoryDTO>> fetchActifs() {
        return ResponseEntity.ok(inventaireService.fetchActifs());
    }

    @GetMapping("/rayons/{id}")
    public ResponseEntity<List<RayonDTO>> getRayons(@PathVariable Long id) {
        return ResponseEntity.ok(inventaireService.fetchRayonsByStoreInventoryId(id));
    }

    @PostMapping("/items/synchronize")
    public ResponseEntity<List<RayonDTO>> synchronizeStoreInventoryLine(@RequestBody List<StoreInventoryLineDTO> storeInventoryLines) {
        inventaireService.synchronizeStoreInventoryLine(storeInventoryLines);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<List<StoreInventoryLineDTO>> getAllItems(@PathVariable Long id) {
        return ResponseEntity.ok(inventaireService.getAllItems(id));
    }

    @GetMapping("/rayons/{rayonId}/items/{id}")
    public ResponseEntity<List<StoreInventoryLineDTO>> getItemsByRayonId(@PathVariable("rayonId") Long rayonId, @PathVariable("id") Long id) {
        return ResponseEntity.ok(inventaireService.getItemsByRayonId(id, rayonId));
    }

}
