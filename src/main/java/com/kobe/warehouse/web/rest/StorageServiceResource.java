package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.StorageDTO;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class StorageServiceResource {

    private final StorageService storageService;

    public StorageServiceResource(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/storages")
    public ResponseEntity<List<StorageDTO>> getAll() {
        return ResponseEntity.ok().body(this.storageService.fetchAllByConnectedUser());
    }
}
