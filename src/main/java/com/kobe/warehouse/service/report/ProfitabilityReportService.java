package com.kobe.warehouse.service.report;

import com.kobe.warehouse.domain.enumeration.BCGCategory;
import com.kobe.warehouse.service.dto.report.ProductProfitabilityDTO;
import com.kobe.warehouse.service.dto.report.ProfitabilitySummaryDTO;
import java.util.List;

public interface ProfitabilityReportService {
    /**
     * Get all product profitability data
     *
     * @return List of product profitability records
     */
    List<ProductProfitabilityDTO> getAllProductProfitability();

    /**
     * Get product profitability filtered by product category
     *
     * @param categorie The category name to filter by
     * @return List of product profitability records for the category
     */
    List<ProductProfitabilityDTO> getProductProfitabilityByCategory(String categorie);

    /**
     * Get product profitability filtered by BCG classification
     *
     * @param bcgCategory The BCG classification to filter by (STAR, CASH_COW, QUESTION_MARK, DOG)
     * @return List of product profitability records for the BCG category
     */
    List<ProductProfitabilityDTO> getProductProfitabilityByBCGCategory(BCGCategory bcgCategory);

    /**
     * Get top N most profitable products
     *
     * @param limit Number of products to return
     * @return List of top profitable products
     */
    List<ProductProfitabilityDTO> getTopProfitableProducts(int limit);

    /**
     * Get low margin products (< 10%)
     *
     * @return List of products with margin below 10%
     */
    List<ProductProfitabilityDTO> getLowMarginProducts();

    /**
     * Get aggregated profitability summary
     *
     * @return Profitability summary with BCG distribution
     */
    ProfitabilitySummaryDTO getProfitabilitySummary();

}
