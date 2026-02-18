package com.kobe.warehouse.service.report.pdf;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.StockValuationView;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.report.StockValuationDTO;
import com.kobe.warehouse.service.dto.report.StockValuationSummaryDTO;
import com.kobe.warehouse.service.report.StockValuationReportService;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StockValuationPdfReportService extends AbstractStatistiqueReportService {
    private final SpringTemplateEngine templateEngine;
    private final Map<String, Object> variablesMap = new HashMap<>();

    private final StockValuationReportService stockValuationReportService;

    public StockValuationPdfReportService(FileStorageProperties fileStorageProperties, StorageService storageService, SpringTemplateEngine templateEngine, StockValuationReportService stockValuationReportService) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
        this.stockValuationReportService = stockValuationReportService;
    }


    @Override
    protected String getTemplateAsHtml() {
        return templateEngine.process("reports/stock-valuation/main", super.getContextVariables());
    }

    @Override
    protected String getTemplateAsHtml(Context context) {
        this.getParameters().forEach(context::setVariable);
        return templateEngine.process("reports/stock-valuation/main", context);
    }

    @Override
    protected Map<String, Object> getParameters() {
        return this.variablesMap;
    }

    @Override
    protected String getGenerateFileName() {
        return "rapport_valuation_stock";
    }


    public byte[] export(Integer familleProduitId, Integer rayonId) {
        List<StockValuationView> valuations = stockValuationReportService.getStockValuation(familleProduitId, rayonId);
        StockValuationSummaryDTO summary = stockValuationReportService.getStockValuationSummary();

        this.getParameters().put("valuations", valuations);
        this.getParameters().put("summary", summary);
        this.getParameters().put("reportTitle", "Rapport de Valorisation du Stock");
        this.getParameters().put("page_count", "1/1");
        super.getCommonParameters();

        return super.exportReportToPdf();
    }
}
