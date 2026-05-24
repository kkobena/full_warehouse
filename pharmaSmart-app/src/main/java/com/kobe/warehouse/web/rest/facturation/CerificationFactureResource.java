package com.kobe.warehouse.web.rest.facturation;

import com.kobe.warehouse.domain.FactureItemId;
import com.kobe.warehouse.service.fne.model.FneResponse;
import com.kobe.warehouse.service.fne.service.FneService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;


@RestController
@RequestMapping("/api/certification-factures")
public class CerificationFactureResource {
    private final FneService fneService;

    public CerificationFactureResource(FneService fneService) {
        this.fneService = fneService;
    }

    @GetMapping("/certifier/{id}/{invoiceDate}")
    public ResponseEntity<FneResponse> certify(
        @PathVariable(name = "id") Long factureItemId,
        @PathVariable(name = "invoiceDate") LocalDate invoiceDate
    ) {
        return ResponseEntity.ok(fneService.create(new FactureItemId(factureItemId, invoiceDate)));
    }

    @GetMapping("/certifier-groupe/{id}/{invoiceDate}")
    public ResponseEntity<Void> certifyGroupInvoice(
        @PathVariable(name = "id") Long factureItemId,
        @PathVariable(name = "invoiceDate") LocalDate invoiceDate
    ) {
        fneService.certifyGroupInvoice(new FactureItemId(factureItemId, invoiceDate));
        return ResponseEntity.ok().build();
    }
}
