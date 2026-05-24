package com.kobe.warehouse.web.rest.report;

import com.kobe.warehouse.service.dto.report.ConcentrationEvolutionDTO;
import com.kobe.warehouse.service.dto.report.ConcentrationOrganismeDTO;
import com.kobe.warehouse.service.dto.report.ConcentrationSummaryDTO;
import com.kobe.warehouse.service.report.ConcentrationPayersService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/concentration-payers")
public class ConcentrationPayersResource {

    private final ConcentrationPayersService service;

    public ConcentrationPayersResource(ConcentrationPayersService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public ResponseEntity<ConcentrationSummaryDTO> getSummary(
        @RequestParam(defaultValue = "quarter") String periode,
        @RequestParam(defaultValue = "10") int topN
    ) {
        return ResponseEntity.ok(service.getSummary(periode, topN));
    }

    @GetMapping("/organismes")
    public ResponseEntity<List<ConcentrationOrganismeDTO>> getOrganismes(
        @RequestParam(defaultValue = "quarter") String periode,
        @RequestParam(defaultValue = "10") int topN
    ) {
        return ResponseEntity.ok(service.getOrganismes(periode, topN));
    }

    @GetMapping("/evolution")
    public ResponseEntity<ConcentrationEvolutionDTO> getEvolution(
        @RequestParam(defaultValue = "10") int topN
    ) {
        return ResponseEntity.ok(service.getEvolution(topN));
    }
}
