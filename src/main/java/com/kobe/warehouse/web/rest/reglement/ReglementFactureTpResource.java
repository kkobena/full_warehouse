package com.kobe.warehouse.web.rest.reglement;

import com.kobe.warehouse.service.reglement.ReglementRegistry;
import com.kobe.warehouse.service.reglement.dto.ReglementParam;
import com.kobe.warehouse.service.reglement.dto.ResponseReglementDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReglementFactureTpResource {

    private final ReglementRegistry reglementRegistry;

    public ReglementFactureTpResource(ReglementRegistry reglementRegistry) {
        this.reglementRegistry = reglementRegistry;
    }

    @PostMapping("/reglement-factures-tp")
    public ResponseEntity<ResponseReglementDTO> doReglement(@Valid @RequestBody ReglementParam reglementParam) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            reglementRegistry.getService(reglementParam.getMode()).doReglement(reglementParam)
        );
    }
}
