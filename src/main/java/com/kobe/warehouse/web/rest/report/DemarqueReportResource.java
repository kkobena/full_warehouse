package com.kobe.warehouse.web.rest.report;

import com.kobe.warehouse.service.dto.report.DemarqueByMotifDTO;
import com.kobe.warehouse.service.dto.report.DemarqueKpiDTO;
import com.kobe.warehouse.service.report.DemarqueReportService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demarque-report")
public class DemarqueReportResource {

    private final DemarqueReportService demarqueReportService;

    public DemarqueReportResource(DemarqueReportService demarqueReportService) {
        this.demarqueReportService = demarqueReportService;
    }

    @GetMapping("/kpi")
    public ResponseEntity<DemarqueKpiDTO> getKpi(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(demarqueReportService.getKpi(startDate, endDate));
    }

    @GetMapping("/by-motif")
    public ResponseEntity<List<DemarqueByMotifDTO>> getByMotif(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(demarqueReportService.getByMotif(startDate, endDate));
    }
}
