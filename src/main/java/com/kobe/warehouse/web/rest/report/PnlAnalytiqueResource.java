package com.kobe.warehouse.web.rest.report;

import com.kobe.warehouse.service.dto.report.PnlEvolutionDTO;
import com.kobe.warehouse.service.dto.report.PnlFamilleDTO;
import com.kobe.warehouse.service.dto.report.PnlSegmentDTO;
import com.kobe.warehouse.service.report.PnlAnalytiqueService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pnl-analytique")
public class PnlAnalytiqueResource {

    private final PnlAnalytiqueService pnlAnalytiqueService;

    public PnlAnalytiqueResource(PnlAnalytiqueService pnlAnalytiqueService) {
        this.pnlAnalytiqueService = pnlAnalytiqueService;
    }

    @GetMapping("/segment")
    public ResponseEntity<List<PnlSegmentDTO>> getSnapshotBySegment(
        @RequestParam(required = false) Integer year
    ) {
        int y = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(pnlAnalytiqueService.getSnapshotBySegment(y));
    }

    @GetMapping("/famille")
    public ResponseEntity<List<PnlFamilleDTO>> getSnapshotByFamille(
        @RequestParam(required = false) Integer year
    ) {
        int y = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(pnlAnalytiqueService.getSnapshotByFamille(y));
    }

    @GetMapping("/evolution")
    public ResponseEntity<PnlEvolutionDTO> getEvolutionByFamille() {
        return ResponseEntity.ok(pnlAnalytiqueService.getEvolutionByFamille());
    }

    @GetMapping("/evolution-segment")
    public ResponseEntity<PnlEvolutionDTO> getEvolutionBySegment() {
        return ResponseEntity.ok(pnlAnalytiqueService.getEvolutionBySegment());
    }
}
