package com.kobe.warehouse.web.rest.stock;

import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.StorageDTO;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.kobe.warehouse.security.navaccess.NavAccessExempt;

@RestController
@RequestMapping("/api")
@NavAccessExempt("emplacements de stockage de l'utilisateur, lus par la vente, le catalogue et l'inventaire")
public class StorageServiceResource {

    private final StorageService storageService;

    public StorageServiceResource(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/storages/user-storages")
    public ResponseEntity<List<StorageDTO>> getAll() {
        return ResponseEntity.ok().body(this.storageService.fetchAllByConnectedUser());
    }

    @GetMapping("/storages")
    public ResponseEntity<List<StorageDTO>> fetchAllByMagasin(@RequestParam(name = "magasinId", required = false) Integer magasinId) {
        return ResponseEntity.ok().body(this.storageService.fetchAllByMagasin(magasinId));
    }
}
