package com.kobe.warehouse.web.rest.report;

import com.kobe.warehouse.service.dto.report.BfrEvolutionDTO;
import com.kobe.warehouse.service.dto.report.BfrSnapshotDTO;
import com.kobe.warehouse.service.report.CashFlowBfrService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cash-flow-bfr")
public class CashFlowBfrResource {

    private final CashFlowBfrService service;

    public CashFlowBfrResource(CashFlowBfrService service) {
        this.service = service;
    }

    @GetMapping("/snapshot")
    public ResponseEntity<BfrSnapshotDTO> getSnapshot() {
        return ResponseEntity.ok(service.getSnapshot());
    }

    @GetMapping("/evolution")
    public ResponseEntity<BfrEvolutionDTO> getEvolution() {
        return ResponseEntity.ok(service.getEvolution());
    }
}
