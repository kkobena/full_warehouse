package com.kobe.warehouse.web.rest.java_client.mobile;

import com.kobe.warehouse.service.mobile.dto.RecapCaisse;
import com.kobe.warehouse.service.mobile.service.MobileCiasseRecapService;
import com.kobe.warehouse.service.tiketz.dto.TicketZParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/java-client/mobile/recap-caisse")
public class MobileCiasseRecapResource {
    private final MobileCiasseRecapService mobileCiasseRecapService;

    public MobileCiasseRecapResource(MobileCiasseRecapService mobileCiasseRecapService) {
        this.mobileCiasseRecapService = mobileCiasseRecapService;
    }

    @GetMapping
    public ResponseEntity<RecapCaisse> getData(TicketZParam param) {
        return ResponseEntity.ok(mobileCiasseRecapService.getRecapCaisse(param));
    }
}
