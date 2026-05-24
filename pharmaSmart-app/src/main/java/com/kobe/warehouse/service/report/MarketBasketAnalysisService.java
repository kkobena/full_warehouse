package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.MarketBasketSummaryDTO;
import com.kobe.warehouse.service.dto.report.ProductAssociationDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for Market Basket Analysis (Cross-selling)
 */
public interface MarketBasketAnalysisService {
    /**
     * Get product associations (products frequently bought together)
     *
     * @param startDate start date for analysis
     * @param endDate end date for analysis
     * @param minSupport minimum support threshold (%)
     * @param minConfidence minimum confidence threshold (%)
     * @param limit maximum number of associations to return
     * @return list of product associations
     */
    List<ProductAssociationDTO> getProductAssociations(
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal minSupport,
        BigDecimal minConfidence,
        Integer limit
    );

    /**
     * Get products frequently bought with a specific product
     *
     * @param productId the product ID
     * @param startDate start date
     * @param endDate end date
     * @param limit maximum results
     * @return list of associated products
     */
    List<ProductAssociationDTO> getAssociationsForProduct(Long productId, LocalDate startDate, LocalDate endDate, Integer limit);

    /**
     * Get summary statistics for market basket analysis
     *
     * @param startDate start date
     * @param endDate end date
     * @return summary statistics
     */
    MarketBasketSummaryDTO getMarketBasketSummary(LocalDate startDate, LocalDate endDate);

    /**
     * Get cross-selling recommendations for a product
     * (similar to getAssociationsForProduct but formatted for UI recommendations)
     *
     * @param productId the product ID
     * @return list of recommended products
     */
    List<ProductAssociationDTO> getCrossSellRecommendations(Long productId);
}
