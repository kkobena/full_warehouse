package com.kobe.warehouse.web.rest.java_client.mobile;

import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.mobile.dto.BalanceWrapper;
import com.kobe.warehouse.service.mobile.service.MobileBalanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/java-client/mobile/balance")
public class MobileBalanceResource {

    private final MobileBalanceService mobileBalanceService;

    public MobileBalanceResource(MobileBalanceService mobileBalanceService) {
        this.mobileBalanceService = mobileBalanceService;
    }

    @GetMapping
    public ResponseEntity<BalanceWrapper> getData(MvtParam mvtParam) {
        return ResponseEntity.ok(mobileBalanceService.getBalanceCaisse(mvtParam));
    }
}
