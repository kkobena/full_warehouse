package com.kobe.warehouse.web.rest.report;

import com.kobe.warehouse.service.dto.report.MarketBasketSummaryDTO;
import com.kobe.warehouse.service.dto.report.ProductAssociationDTO;
import com.kobe.warehouse.service.report.MarketBasketAnalysisService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Market Basket Analysis (Cross-selling)
 */
@RestController
@RequestMapping("/api/market-basket")
public class MarketBasketAnalysisResource {

    private final MarketBasketAnalysisService marketBasketAnalysisService;

    public MarketBasketAnalysisResource(MarketBasketAnalysisService marketBasketAnalysisService) {
        this.marketBasketAnalysisService = marketBasketAnalysisService;
    }

    /**
     * GET /api/market-basket/associations : Get product associations
     *
     * @param startDate start date for analysis
     * @param endDate end date for analysis
     * @param minSupport minimum support threshold (default 1%)
     * @param minConfidence minimum confidence threshold (default 10%)
     * @param limit maximum number of results (default 50)
     * @return list of product associations
     */
    @GetMapping("/associations")
    public ResponseEntity<List<ProductAssociationDTO>> getProductAssociations(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(defaultValue = "1.0") BigDecimal minSupport,
        @RequestParam(defaultValue = "10.0") BigDecimal minConfidence,
        @RequestParam(defaultValue = "50") Integer limit
    ) {
        List<ProductAssociationDTO> associations = marketBasketAnalysisService.getProductAssociations(
            startDate,
            endDate,
            minSupport,
            minConfidence,
            limit
        );
        return ResponseEntity.ok(associations);
    }

    /**
     * GET /api/market-basket/associations/{productId} : Get associations for a specific product
     *
     * @param productId the product ID
     * @param startDate start date
     * @param endDate end date
     * @param limit maximum results (default 20)
     * @return list of associated products
     */
    @GetMapping("/associations/{productId}")
    public ResponseEntity<List<ProductAssociationDTO>> getAssociationsForProduct(
        @PathVariable Long productId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(defaultValue = "20") Integer limit
    ) {
        List<ProductAssociationDTO> associations = marketBasketAnalysisService.getAssociationsForProduct(
            productId,
            startDate,
            endDate,
            limit
        );
        return ResponseEntity.ok(associations);
    }

    /**
     * GET /api/market-basket/summary : Get market basket summary
     *
     * @param startDate start date
     * @param endDate end date
     * @return summary statistics
     */
    @GetMapping("/summary")
    public ResponseEntity<MarketBasketSummaryDTO> getMarketBasketSummary(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        MarketBasketSummaryDTO summary = marketBasketAnalysisService.getMarketBasketSummary(startDate, endDate);
        return ResponseEntity.ok(summary);
    }

    /**
     * GET /api/market-basket/recommendations/{productId} : Get cross-sell recommendations
     *
     * @param productId the product ID
     * @return list of recommended products
     */
    @GetMapping("/recommendations/{productId}")
    public ResponseEntity<List<ProductAssociationDTO>> getCrossSellRecommendations(@PathVariable Long productId) {
        List<ProductAssociationDTO> recommendations = marketBasketAnalysisService.getCrossSellRecommendations(productId);
        return ResponseEntity.ok(recommendations);
    }
}
