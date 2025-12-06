package com.kobe.warehouse.web.rest.report;

import com.kobe.warehouse.domain.enumeration.BCGCategory;
import com.kobe.warehouse.service.dto.report.ProductProfitabilityDTO;
import com.kobe.warehouse.service.dto.report.ProfitabilitySummaryDTO;
import com.kobe.warehouse.service.report.ProfitabilityReportService;
import com.kobe.warehouse.service.report.pdf.ProfitabilityPdfReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProfitabilityReportResource {

    private final ProfitabilityReportService profitabilityReportService;
    private final ProfitabilityPdfReportService profitabilityPdfReportService;

    public ProfitabilityReportResource(ProfitabilityReportService profitabilityReportService, ProfitabilityPdfReportService profitabilityPdfReportService) {
        this.profitabilityReportService = profitabilityReportService;
        this.profitabilityPdfReportService = profitabilityPdfReportService;
    }

    /**
     * GET /profitability : Get all product profitability data
     *
     * @return List of product profitability records
     */
    @GetMapping("/profitability")
    public ResponseEntity<List<ProductProfitabilityDTO>> getAllProductProfitability() {
        List<ProductProfitabilityDTO> profitability = profitabilityReportService.getAllProductProfitability();
        return ResponseEntity.ok().body(profitability);
    }

    /**
     * GET /profitability/category : Get product profitability by category
     *
     * @param categorie The category name to filter by
     * @return List of product profitability records for the category
     */
    @GetMapping("/profitability/category")
    public ResponseEntity<List<ProductProfitabilityDTO>> getProductProfitabilityByCategory(@RequestParam String categorie) {
        List<ProductProfitabilityDTO> profitability = profitabilityReportService.getProductProfitabilityByCategory(categorie);
        return ResponseEntity.ok().body(profitability);
    }

    /**
     * GET /profitability/bcg : Get product profitability by BCG classification
     *
     * @param bcgCategory The BCG classification to filter by (STAR, CASH_COW, QUESTION_MARK, DOG)
     * @return List of product profitability records for the BCG category
     */
    @GetMapping("/profitability/bcg")
    public ResponseEntity<List<ProductProfitabilityDTO>> getProductProfitabilityByBCGCategory(@RequestParam BCGCategory bcgCategory) {
        List<ProductProfitabilityDTO> profitability = profitabilityReportService.getProductProfitabilityByBCGCategory(bcgCategory);
        return ResponseEntity.ok().body(profitability);
    }

    /**
     * GET /profitability/top : Get top N most profitable products
     *
     * @param limit Number of products to return (default: 20)
     * @return List of top profitable products
     */
    @GetMapping("/profitability/top")
    public ResponseEntity<List<ProductProfitabilityDTO>> getTopProfitableProducts(
        @RequestParam(defaultValue = "20") int limit
    ) {
        List<ProductProfitabilityDTO> topProducts = profitabilityReportService.getTopProfitableProducts(limit);
        return ResponseEntity.ok().body(topProducts);
    }

    /**
     * GET /profitability/low-margin : Get low margin products (< 10%)
     *
     * @return List of products with margin below 10%
     */
    @GetMapping("/profitability/low-margin")
    public ResponseEntity<List<ProductProfitabilityDTO>> getLowMarginProducts() {
        List<ProductProfitabilityDTO> lowMargin = profitabilityReportService.getLowMarginProducts();
        return ResponseEntity.ok().body(lowMargin);
    }

    /**
     * GET /profitability/summary : Get aggregated profitability summary
     *
     * @return Profitability summary with BCG distribution
     */
    @GetMapping("/profitability/summary")
    public ResponseEntity<ProfitabilitySummaryDTO> getProfitabilitySummary() {
        ProfitabilitySummaryDTO summary = profitabilityReportService.getProfitabilitySummary();
        return ResponseEntity.ok().body(summary);
    }

    /**
     * GET /profitability/export : Export profitability report as PDF
     *
     * @return PDF file
     */
    @GetMapping(value = "/profitability/export", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportProfitabilityToPdf() {

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=profitability-report.pdf");
        return ResponseEntity.ok().headers(headers).body(profitabilityPdfReportService.export());
    }
}
