package com.kobe.warehouse.web.rest.report;

import com.kobe.warehouse.domain.StockValuationView;
import com.kobe.warehouse.service.dto.report.StockValuationSummaryDTO;
import com.kobe.warehouse.service.report.StockValuationReportService;
import com.kobe.warehouse.service.report.pdf.StockValuationPdfReportService;
import com.kobe.warehouse.web.util.PaginationUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

import static java.util.Objects.isNull;

@RestController
@RequestMapping("/api")
public class StockValuationReportResource {

    private final StockValuationReportService stockValuationReportService;
    private final StockValuationPdfReportService stockValuationPdfReportService;

    public StockValuationReportResource(StockValuationReportService stockValuationReportService, StockValuationPdfReportService stockValuationPdfReportService) {
        this.stockValuationReportService = stockValuationReportService;
        this.stockValuationPdfReportService = stockValuationPdfReportService;
    }

    /**
     * GET /stock/valuation : Get all stock valuation data
     *
     * @return List of stock valuation records
     */

    @GetMapping("/stock/valuation")
    public ResponseEntity<List<StockValuationView>> getAllStockValuation(Pageable pageable,
                                                                         @RequestParam(value = "familleProduitId", required = false) Integer familleProduitId,
                                                                         @RequestParam(value = "rayonId", required = false) Integer rayonId

    ) {

        Page<StockValuationView> page = stockValuationReportService.getStockValuationPaginated(familleProduitId, rayonId, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }


    /**
     * GET /stock/valuation/summary : Get aggregated stock valuation summary
     *
     * @return Summary with total values and averages
     */
    @GetMapping("/stock/valuation/summary")
    public ResponseEntity<StockValuationSummaryDTO> getStockValuationSummary(@RequestParam(value = "familleProduitId", required = false) Integer familleProduitId,
                                                                             @RequestParam(value = "rayonId", required = false) Integer rayonId) {
        if (isNull(familleProduitId) && isNull(rayonId)) {
            return ResponseEntity.ok(stockValuationReportService.getStockValuationSummary());
        } else {
            return ResponseEntity.ok(stockValuationReportService.getStockValuationSummary(familleProduitId, rayonId));
        }
    }

    /**
     * GET /stock/valuation/export : Export stock valuation report as PDF
     *
     * @return PDF file
     */
    @GetMapping(value = "/stock/valuation/export", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportStockValuationToPdf(@RequestParam(value = "familleProduitId", required = false) Integer familleProduitId,
                                                            @RequestParam(value = "rayonId", required = false) Integer rayonId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=stock-valuation.pdf");
        return ResponseEntity.ok().headers(headers).body(stockValuationPdfReportService.export(familleProduitId, rayonId));
    }
}
