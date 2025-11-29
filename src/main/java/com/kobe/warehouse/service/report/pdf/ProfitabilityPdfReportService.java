package com.kobe.warehouse.service.report.pdf;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.report.ProductProfitabilityDTO;
import com.kobe.warehouse.service.dto.report.ProfitabilitySummaryDTO;
import com.kobe.warehouse.service.report.ProfitabilityReportService;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProfitabilityPdfReportService extends AbstractStatistiqueReportService {
    private final SpringTemplateEngine templateEngine;
    private final Map<String, Object> variablesMap = new HashMap<>();
    private final ProfitabilityReportService profitabilityReportService;

    public ProfitabilityPdfReportService(FileStorageProperties fileStorageProperties, StorageService storageService, SpringTemplateEngine templateEngine, ProfitabilityReportService profitabilityReportService) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
        this.profitabilityReportService = profitabilityReportService;
    }


    @Override
    protected String getTemplateAsHtml() {
        return templateEngine.process("reports/profitability/main", super.getContextVariables());
    }

    @Override
    protected String getTemplateAsHtml(Context context) {
        this.getParameters().forEach(context::setVariable);
        return templateEngine.process("reports/profitability/main", context);
    }

    @Override
    protected Map<String, Object> getParameters() {
        return this.variablesMap;
    }

    @Override
    protected String getGenerateFileName() {
        return "rapport_rentabilite_produit";
    }


    public byte[] export() {

        List<ProductProfitabilityDTO> products = profitabilityReportService.getAllProductProfitability();
        ProfitabilitySummaryDTO summary = profitabilityReportService.getProfitabilitySummary();
        this.getParameters().put("products", products);
        this.getParameters().put("summary", summary);
        this.getParameters().put("reportTitle", "Rapport de Rentabilité (Analyse BCG)");
        this.getParameters().put("page_count", "1/1");

        super.getCommonParameters();
        return super.exportReportToPdf();
    }
}
