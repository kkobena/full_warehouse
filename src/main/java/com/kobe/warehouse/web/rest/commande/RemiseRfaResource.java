package com.kobe.warehouse.web.rest.commande;

import com.kobe.warehouse.service.ap.RemiseRfaService;
import com.kobe.warehouse.service.dto.AvoirFournisseurRfaDTO;
import com.kobe.warehouse.service.dto.RemiseRfaFournisseurDTO;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/remises-rfa")
public class RemiseRfaResource {

    private final RemiseRfaService remiseRfaService;

    public RemiseRfaResource(RemiseRfaService remiseRfaService) {
        this.remiseRfaService = remiseRfaService;
    }

    @GetMapping
    public ResponseEntity<List<RemiseRfaFournisseurDTO>> getRfaFournisseurs() {
        return ResponseEntity.ok(remiseRfaService.getRfaFournisseurs());
    }

    @GetMapping("/avoirs")
    public ResponseEntity<List<AvoirFournisseurRfaDTO>> getAvoirsFournisseurs() {
        return ResponseEntity.ok(remiseRfaService.getAvoirsFournisseurs());
    }
}
