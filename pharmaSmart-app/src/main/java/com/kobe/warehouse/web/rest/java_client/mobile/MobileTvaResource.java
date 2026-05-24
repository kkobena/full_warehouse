package com.kobe.warehouse.web.rest.java_client.mobile;

import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.mobile.dto.Tva;
import com.kobe.warehouse.service.mobile.service.MobileTvaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/java-client/mobile/tva")
public class MobileTvaResource {

    private final MobileTvaService mobileTvaService;

    public MobileTvaResource(MobileTvaService mobileTvaService) {
        this.mobileTvaService = mobileTvaService;
    }

    @GetMapping
    public ResponseEntity<Tva> getData(MvtParam mvtParam) {
        return ResponseEntity.ok(mobileTvaService.getTva(mvtParam));
    }
}
