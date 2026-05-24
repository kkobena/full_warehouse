package com.kobe.warehouse.service.report;

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
     * Get aggregated profitability summary
     *
     * @return Profitability summary with BCG distribution
     */
    ProfitabilitySummaryDTO getProfitabilitySummary();

}
