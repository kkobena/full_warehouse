package com.kobe.warehouse.service.report;

import com.kobe.warehouse.domain.StockValuationView;
import com.kobe.warehouse.service.dto.report.StockValuationSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StockValuationReportService {

    /**
     * Get aggregated stock valuation summary
     *
     * @return Summary with total values and averages
     */
    StockValuationSummaryDTO getStockValuationSummary();

    Page<StockValuationView> getStockValuationPaginated(Integer familleProduitId, Integer rayonId, Pageable pageable);

    List<StockValuationView> getStockValuation(Integer familleProduitId, Integer rayonId);
    StockValuationSummaryDTO getStockValuationSummary(Integer familleProduitId, Integer rayonId);

}
