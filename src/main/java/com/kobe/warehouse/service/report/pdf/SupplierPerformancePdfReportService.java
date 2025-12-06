package com.kobe.warehouse.service.report.pdf;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.report.SupplierPerformanceDTO;
import com.kobe.warehouse.service.dto.report.SupplierPerformanceSummaryDTO;
import com.kobe.warehouse.service.report.SupplierPerformanceReportService;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SupplierPerformancePdfReportService extends AbstractStatistiqueReportService {
    private final SpringTemplateEngine templateEngine;
    private final Map<String, Object> variablesMap = new HashMap<>();

    private final SupplierPerformanceReportService supplierPerformanceReportService;

    public SupplierPerformancePdfReportService(FileStorageProperties fileStorageProperties, StorageService storageService, SpringTemplateEngine templateEngine, SupplierPerformanceReportService supplierPerformanceReportService) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
        this.supplierPerformanceReportService = supplierPerformanceReportService;
    }


    @Override
    protected String getTemplateAsHtml() {
        return templateEngine.process("reports/supplier-performance/main", super.getContextVariables());
    }

    @Override
    protected String getTemplateAsHtml(Context context) {
        this.getParameters().forEach(context::setVariable);
        return templateEngine.process("reports/supplier-performance/main", context);
    }

    @Override
    protected Map<String, Object> getParameters() {
        return this.variablesMap;
    }

    @Override
    protected String getGenerateFileName() {
        return "rapport_performance_fournisseur";
    }


    public byte[] export() {
        List<SupplierPerformanceDTO> suppliers = supplierPerformanceReportService.getAllSupplierPerformance();
        SupplierPerformanceSummaryDTO summary = supplierPerformanceReportService.getSupplierPerformanceSummary();
        this.getParameters().put("suppliers", suppliers);
        this.getParameters().put("summary", summary);
        this.getParameters().put("reportTitle", "Rapport de Performance Fournisseurs");
        this.getParameters().put("page_count", "1/1");


        super.getCommonParameters();
        return super.exportReportToPdf();
    }
}
