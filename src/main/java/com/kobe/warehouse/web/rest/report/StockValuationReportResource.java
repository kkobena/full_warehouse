package com.kobe.warehouse.web.rest.report;

import com.kobe.warehouse.service.dto.report.StockValuationDTO;
import com.kobe.warehouse.service.dto.report.StockValuationSummaryDTO;
import com.kobe.warehouse.service.report.StockValuationReportService;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class StockValuationReportResource {

    private final StockValuationReportService stockValuationReportService;

    public StockValuationReportResource(StockValuationReportService stockValuationReportService) {
        this.stockValuationReportService = stockValuationReportService;
    }

    /**
     * GET /stock/valuation : Get all stock valuation data
     *
     * @return List of stock valuation records
     */
    @GetMapping("/stock/valuation")
    public ResponseEntity<List<StockValuationDTO>> getAllStockValuation() {
        List<StockValuationDTO> valuations = stockValuationReportService.getAllStockValuation();
        return ResponseEntity.ok().body(valuations);
    }

    /**
     * GET /stock/valuation/category : Get stock valuation by category
     *
     * @param categorie The category name to filter by
     * @return List of stock valuation records for the category
     */
    @GetMapping("/stock/valuation/category")
    public ResponseEntity<List<StockValuationDTO>> getStockValuationByCategory(@RequestParam String categorie) {
        List<StockValuationDTO> valuations = stockValuationReportService.getStockValuationByCategory(categorie);
        return ResponseEntity.ok().body(valuations);
    }

    /**
     * GET /stock/valuation/storage : Get stock valuation by storage location
     *
     * @param storageLocation The storage location name to filter by
     * @return List of stock valuation records for the storage location
     */
    @GetMapping("/stock/valuation/storage")
    public ResponseEntity<List<StockValuationDTO>> getStockValuationByStorage(@RequestParam String storageLocation) {
        List<StockValuationDTO> valuations = stockValuationReportService.getStockValuationByStorage(storageLocation);
        return ResponseEntity.ok().body(valuations);
    }

    /**
     * GET /stock/valuation/summary : Get aggregated stock valuation summary
     *
     * @return Summary with total values and averages
     */
    @GetMapping("/stock/valuation/summary")
    public ResponseEntity<StockValuationSummaryDTO> getStockValuationSummary() {
        StockValuationSummaryDTO summary = stockValuationReportService.getStockValuationSummary();
        return ResponseEntity.ok().body(summary);
    }

    /**
     * GET /stock/valuation/export : Export stock valuation report as PDF
     *
     * @return PDF file
     */
    @GetMapping(value = "/stock/valuation/export", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportStockValuationToPdf() {
        byte[] pdf = stockValuationReportService.exportStockValuationToPdf();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=stock-valuation.pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
