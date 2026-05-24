package com.kobe.warehouse.web.rest.java_client.mobile;

import com.kobe.warehouse.service.dto.VenteRecordParamDTO;
import com.kobe.warehouse.service.mobile.dto.Dashboard;
import com.kobe.warehouse.service.mobile.service.MobileDashoardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/java-client/mobile/dashboard")
public class MobileDashoardResource {

    private final MobileDashoardService mobileDashoardService;

    public MobileDashoardResource(MobileDashoardService mobileDashoardService) {
        this.mobileDashoardService = mobileDashoardService;
    }

    @GetMapping("/data")
    public ResponseEntity<Dashboard> getData(VenteRecordParamDTO venteRecordParam) {
        return ResponseEntity.ok(mobileDashoardService.getData(venteRecordParam));
    }
}
