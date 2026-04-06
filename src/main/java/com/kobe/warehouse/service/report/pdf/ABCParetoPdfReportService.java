package com.kobe.warehouse.service.report.pdf;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.report.ABCParetoDTO;
import com.kobe.warehouse.service.dto.report.ABCParetoSummaryDTO;
import com.kobe.warehouse.service.report.ABCParetoReportService;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ABCParetoPdfReportService extends AbstractStatistiqueReportService {
    private final SpringTemplateEngine templateEngine;
    private final Map<String, Object> variablesMap = new HashMap<>();

    private final ABCParetoReportService abcParetoReportService;

    public ABCParetoPdfReportService(FileStorageProperties fileStorageProperties, StorageService storageService, SpringTemplateEngine templateEngine, ABCParetoReportService abcParetoReportService) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
        this.abcParetoReportService = abcParetoReportService;
    }


    @Override
    protected String getTemplateAsHtml() {
        return templateEngine.process("reports/abc-pareto/main", super.getContextVariables());
    }

    @Override
    protected String getTemplateAsHtml(Context context) {
        this.getParameters().forEach(context::setVariable);
        return templateEngine.process("reports/abc-pareto/main", context);
    }

    @Override
    protected Map<String, Object> getParameters() {
        return this.variablesMap;
    }

    @Override
    protected String getGenerateFileName() {
        return "Analyse_ABC_Pareto";
    }


    public byte[] export() {
        List<ABCParetoDTO> products = abcParetoReportService.getAllABCParetoAnalysis();
        ABCParetoSummaryDTO summary = abcParetoReportService.getABCParetoSummary();
        this.getParameters().put("products", products);
        this.getParameters().put("summary", summary);
        this.getParameters().put("reportTitle", "Analyse ABC Pareto (Règle 80/20)");
        super.getCommonParameters();

        return super.exportReportToPdf();
    }
}
