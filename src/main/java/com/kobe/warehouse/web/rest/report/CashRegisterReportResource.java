package com.kobe.warehouse.web.rest.report;

import com.kobe.warehouse.service.dto.report.CashMovementDTO;
import com.kobe.warehouse.service.dto.report.DailyCashRegisterReportDTO;
import com.kobe.warehouse.service.report.CashRegisterReportService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CashRegisterReportResource {

    private final CashRegisterReportService cashRegisterReportService;

    public CashRegisterReportResource(CashRegisterReportService cashRegisterReportService) {
        this.cashRegisterReportService = cashRegisterReportService;
    }

    /**
     * GET /cash-register/daily-report : Get daily cash register report
     *
     * @param date The date for the report (defaults to today)
     * @return Daily cash register report
     */
    @GetMapping("/cash-register/daily-report")
    public ResponseEntity<List<DailyCashRegisterReportDTO>> getDailyReport(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate reportDate = date != null ? date : LocalDate.now();
        List<DailyCashRegisterReportDTO> report = cashRegisterReportService.getDailyReport(reportDate);
        return ResponseEntity.ok().body(report);
    }

    /**
     * GET /cash-register/movements : Get cash movements history
     *
     * @param startDate Start date
     * @param endDate End date
     * @param userId Optional user ID filter
     * @param cashRegisterId Optional cash register ID filter
     * @return List of cash movements
     */
    @GetMapping("/cash-register/movements")
    public ResponseEntity<List<CashMovementDTO>> getCashMovements(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) Long cashRegisterId
    ) {
        List<CashMovementDTO> movements = cashRegisterReportService.getCashMovements(startDate, endDate, userId, cashRegisterId);
        return ResponseEntity.ok().body(movements);
    }

    /**
     * GET /cash-register/summary : Get cash register summary for a period
     *
     * @param startDate Start date (defaults to beginning of current week)
     * @param endDate End date (defaults to today)
     * @return List of daily cash register summaries
     */
    @GetMapping("/cash-register/summary")
    public ResponseEntity<List<DailyCashRegisterReportDTO>> getCashRegisterSummary(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(7);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        List<DailyCashRegisterReportDTO> summary = cashRegisterReportService.getCashRegisterSummary(start, end);
        return ResponseEntity.ok().body(summary);
    }

    /**
     * GET /cash-register/daily-report/export : Export daily cash register report as PDF
     *
     * @param date The date for the report (defaults to today)
     * @return PDF file
     */
    @GetMapping(value = "/cash-register/daily-report/export", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportDailyReportToPdf(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate reportDate = date != null ? date : LocalDate.now();
        byte[] pdf = cashRegisterReportService.exportDailyReportToPdf(reportDate);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=cash-register-report-" + reportDate + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
