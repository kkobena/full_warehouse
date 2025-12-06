package com.kobe.warehouse.service.report.pdf;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.report.TiersPayantCreancesSummaryDTO;
import com.kobe.warehouse.service.dto.report.TiersPayantInvoiceDTO;
import com.kobe.warehouse.service.report.TiersPayantReportService;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TiersPayantPdfReportService extends AbstractStatistiqueReportService {
    private final SpringTemplateEngine templateEngine;
    private final Map<String, Object> variablesMap = new HashMap<>();

    private final TiersPayantReportService tiersPayantReportService;

    public TiersPayantPdfReportService(FileStorageProperties fileStorageProperties, StorageService storageService, SpringTemplateEngine templateEngine, TiersPayantReportService tiersPayantReportService) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
        this.tiersPayantReportService = tiersPayantReportService;
    }


    @Override
    protected String getTemplateAsHtml() {
        return templateEngine.process("reports/tiers-payant/main", super.getContextVariables());
    }

    @Override
    protected String getTemplateAsHtml(Context context) {
        this.getParameters().forEach(context::setVariable);
        return templateEngine.process("reports/tiers-payant/main", context);
    }

    @Override
    protected Map<String, Object> getParameters() {
        return this.variablesMap;
    }

    @Override
    protected String getGenerateFileName() {
        return "rapport_creances_tiers_payants";
    }


    public byte[] export() {
        List<TiersPayantCreancesSummaryDTO> summary = tiersPayantReportService.getCreancesSummary();
        List<TiersPayantInvoiceDTO> invoices = tiersPayantReportService.getUnpaidInvoices(null, null);

        // Calculate total
        int totalCreances = summary.stream().mapToInt(s -> s.montantTotal() != null ? s.montantTotal() : 0).sum();

        this.getParameters().put("summary", summary);
        this.getParameters().put("invoices", invoices);
        this.getParameters().put("totalCreances", totalCreances);
        this.getParameters().put("reportTitle", "Rapport Créances Tiers-Payants");
        this.getParameters().put("page_count", "1/1");
        super.getCommonParameters();

        return super.exportReportToPdf();
    }
}
