package com.kobe.warehouse.service.report.pdf;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.report.ComparativeByTypeDTO;
import com.kobe.warehouse.service.dto.report.ComparativeCADTO;
import com.kobe.warehouse.service.dto.report.ComparativeSummaryDTO;
import com.kobe.warehouse.service.report.ComparativeReportService;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ComparativePdfReportService extends AbstractStatistiqueReportService {
    private final SpringTemplateEngine templateEngine;
    private final Map<String, Object> variablesMap = new HashMap<>();
    private final ComparativeReportService comparativeReportService;

    public ComparativePdfReportService(FileStorageProperties fileStorageProperties, StorageService storageService, SpringTemplateEngine templateEngine, ComparativeReportService comparativeReportService) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
        this.comparativeReportService = comparativeReportService;
    }


    @Override
    protected String getTemplateAsHtml() {
        return templateEngine.process("reports/comparative/main", super.getContextVariables());
    }

    @Override
    protected String getTemplateAsHtml(Context context) {
        this.getParameters().forEach(context::setVariable);
        return templateEngine.process("reports/comparative/main", context);
    }

    @Override
    protected Map<String, Object> getParameters() {
        return this.variablesMap;
    }

    @Override
    protected String getGenerateFileName() {
        return "rapport_comparatif_ventes";
    }


    public byte[] export(String comparisonType, Integer year) {

        ComparativeSummaryDTO summary = comparativeReportService.getComparativeSummary();
        List<ComparativeCADTO> data;

        switch (comparisonType.toUpperCase()) {
            case "MONTHLY":
                data = comparativeReportService.getMonthlyComparison(year);
                break;
            case "QUARTERLY":
                data = comparativeReportService.getQuarterlyComparison(year);
                break;
            case "YEARLY":
                LocalDate start = LocalDate.of(year - 5, 1, 1);
                LocalDate end = LocalDate.of(year, 12, 31);
                data = comparativeReportService.getYearlyComparison(start, end);
                break;
            default:
                data = comparativeReportService.getMonthlyComparison(year);
        }

        List<ComparativeByTypeDTO> byType = comparativeReportService.getComparisonBySalesType(year, year - 1);


        this.getParameters().put("summary", summary);
        this.getParameters().put("comparisons", data);
        this.getParameters().put("byType", byType);
        this.getParameters().put("comparisonType", comparisonType);
        this.getParameters().put("year", year);
        this.getParameters().put("reportTitle", "Rapport Comparatif des Ventes");

        super.getCommonParameters();
        return super.exportReportToPdf();
    }
}
