package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.service.dto.LotDTO;
import com.kobe.warehouse.service.dto.LotJsonValue;
import com.kobe.warehouse.service.stock.LotService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class LotResource {

    private final LotService lotService;

    public LotResource(LotService lotService) {
        this.lotService = lotService;
    }

    @PostMapping("/lot/add-to-commande")
    public ResponseEntity<LotDTO> addLotToCommande(@Valid @RequestBody LotDTO lot) {
        return ResponseEntity.ok(lotService.addLot(lot));
    }

    @PutMapping("/lot/remove-to-commande")
    public ResponseEntity<Void> removeLotToCommande(@RequestBody LotDTO lot) {
        lotService.remove(lot);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/lot/add")
    public ResponseEntity<LotDTO> add(@Valid @RequestBody LotDTO lot) {
        return ResponseEntity.ok().body(lotService.addLot(lot));
    }

    @PostMapping("/lot/edit")
    public ResponseEntity<LotDTO> edit(@Valid @RequestBody LotDTO lot) {
        return ResponseEntity.ok().body(lotService.editLot(lot));
    }

    @DeleteMapping("/lot/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        lotService.remove(id);
        return ResponseEntity.ok().build();
    }
}
