package com.kobe.warehouse.web.rest.report;

import com.kobe.warehouse.service.dto.report.StockAlertDTO;
import com.kobe.warehouse.service.report.StockAlertReportService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class StockAlertReportResource {

    private final StockAlertReportService stockAlertReportService;

    public StockAlertReportResource(StockAlertReportService stockAlertReportService) {
        this.stockAlertReportService = stockAlertReportService;
    }

    /**
     * GET /stock/alerts : Get all stock alerts
     *
     * @param types Optional list of alert types to filter by (RUPTURE, ALERTE, PEREMPTION)
     * @return List of stock alerts
     */
    @GetMapping("/stock/alerts")
    public ResponseEntity<List<StockAlertDTO>> getStockAlerts(
        @RequestParam(required = false) List<StockAlertDTO.StockAlertType> types
    ) {
        List<StockAlertDTO> alerts = stockAlertReportService.getStockAlerts(types);
        return ResponseEntity.ok().body(alerts);
    }

    /**
     * GET /stock/alerts/count : Get count of stock alerts by type
     *
     * @return Map of alert type to count
     */
    @GetMapping("/stock/alerts/count")
    public ResponseEntity<Map<StockAlertDTO.StockAlertType, Long>> getStockAlertsCount() {
        Map<StockAlertDTO.StockAlertType, Long> counts = stockAlertReportService.getStockAlertsCount();
        return ResponseEntity.ok().body(counts);
    }

    /**
     * GET /stock/alerts/export : Export stock alerts report as PDF
     *
     * @param types Optional list of alert types to filter by
     * @return PDF file
     */
    @GetMapping(value = "/stock/alerts/export", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportStockAlertsToPdf(@RequestParam(required = false) List<StockAlertDTO.StockAlertType> types) {
        byte[] pdf = stockAlertReportService.exportStockAlertsToPdf(types);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=stock-alerts.pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
