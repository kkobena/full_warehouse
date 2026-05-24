package com.kobe.warehouse.web.rest.commande;

import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.service.dto.ReconciliationFactureDTO;
import com.kobe.warehouse.service.stock.ReconciliationFournisseurService;
import com.kobe.warehouse.service.stock.ReconciliationFournisseurService.ReconciliationCommand;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bons")
public class ReconciliationFournisseurResource {

    private final ReconciliationFournisseurService reconciliationService;

    public ReconciliationFournisseurResource(ReconciliationFournisseurService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @PostMapping("/{id}/{date}/reconciliation")
    public ResponseEntity<ReconciliationFactureDTO> save(
        @PathVariable Integer id,
        @PathVariable @DateTimeFormat(iso = ISO.DATE) LocalDate date,
        @RequestBody ReconciliationCommand command
    ) {
        ReconciliationFactureDTO result = reconciliationService.save(new CommandeId(id, date), command);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/{date}/reconciliation")
    public ResponseEntity<ReconciliationFactureDTO> find(
        @PathVariable Integer id,
        @PathVariable @DateTimeFormat(iso = ISO.DATE) LocalDate date
    ) {
        ReconciliationFactureDTO dto = reconciliationService.findByCommandeId(new CommandeId(id, date));
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.noContent().build();
    }
}
