package com.kobe.warehouse.web.rest.scheduler;

import com.kobe.warehouse.domain.ScheduledReport;
import com.kobe.warehouse.service.scheduler.ScheduledReportService;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing scheduled reports
 */
@RestController
@RequestMapping("/api/scheduled-reports")
public class ScheduledReportResource {

    private final ScheduledReportService scheduledReportService;

    public ScheduledReportResource(ScheduledReportService scheduledReportService) {
        this.scheduledReportService = scheduledReportService;
    }

    /**
     * GET /api/scheduled-reports : Get all scheduled reports
     */
    @GetMapping("")
    public ResponseEntity<List<ScheduledReport>> getAllScheduledReports() {
        List<ScheduledReport> reports = scheduledReportService.getAllScheduledReports();
        return ResponseEntity.ok(reports);
    }

    /**
     * GET /api/scheduled-reports/active : Get all active scheduled reports
     */
    @GetMapping("/active")
    public ResponseEntity<List<ScheduledReport>> getActiveScheduledReports() {
        List<ScheduledReport> reports = scheduledReportService.getActiveScheduledReports();
        return ResponseEntity.ok(reports);
    }

    /**
     * POST /api/scheduled-reports : Create a new scheduled report
     */
    @PostMapping("")
    public ResponseEntity<ScheduledReport> createScheduledReport(@Valid @RequestBody ScheduledReport scheduledReport)
        throws URISyntaxException {
        ScheduledReport result = scheduledReportService.createScheduledReport(scheduledReport);
        return ResponseEntity.created(new URI("/api/scheduled-reports/" + result.getId())).body(result);
    }

    /**
     * PUT /api/scheduled-reports/:id : Update an existing scheduled report
     */
    @PutMapping("/{id}")
    public ResponseEntity<ScheduledReport> updateScheduledReport(
        @PathVariable Integer id,
        @Valid @RequestBody ScheduledReport scheduledReport
    ) {
        scheduledReport.setId(id);
        ScheduledReport result = scheduledReportService.updateScheduledReport(scheduledReport);
        return ResponseEntity.ok(result);
    }

    /**
     * DELETE /api/scheduled-reports/:id : Delete a scheduled report
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScheduledReport(@PathVariable Integer id) {
        scheduledReportService.deleteScheduledReport(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/scheduled-reports/:id/execute : Execute a scheduled report immediately
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<Void> executeScheduledReport(@PathVariable Integer id) throws Exception {
        ScheduledReport report = scheduledReportService.getAllScheduledReports().stream()
            .filter(r -> r.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Scheduled report not found"));

        scheduledReportService.executeReport(report);
        return ResponseEntity.ok().build();
    }
}
