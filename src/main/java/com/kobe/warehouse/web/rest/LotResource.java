package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.service.dto.LotJsonValue;
import com.kobe.warehouse.service.stock.LotService;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
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
  public ResponseEntity<LotJsonValue> addLotToCommande(@Valid @RequestBody LotJsonValue lot) {

    return ResponseEntity.ok(lotService.addLot(lot));
  }

  @PutMapping("/lot/remove-to-commande")
  public ResponseEntity<Void> removeLotToCommande(@RequestBody LotJsonValue lot) {
    lotService.remove(lot);
    return ResponseEntity.ok().build();
  }
}
