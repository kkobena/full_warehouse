package com.kobe.warehouse.service.report.pdf;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.report.DailyCashRegisterReportDTO;
import com.kobe.warehouse.service.report.CashRegisterReportService;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CashRegisterPdfReportService extends AbstractStatistiqueReportService {
    private final SpringTemplateEngine templateEngine;
    private final Map<String, Object> variablesMap = new HashMap<>();
    private final CashRegisterReportService cashRegisterReportService;

    public CashRegisterPdfReportService(FileStorageProperties fileStorageProperties, StorageService storageService, SpringTemplateEngine templateEngine, CashRegisterReportService cashRegisterReportService) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
        this.cashRegisterReportService = cashRegisterReportService;
    }


    @Override
    protected String getTemplateAsHtml() {
        return templateEngine.process("reports/cash-register/main", super.getContextVariables());
    }

    @Override
    protected String getTemplateAsHtml(Context context) {
        this.getParameters().forEach(context::setVariable);
        return templateEngine.process("reports/cash-register/main", context);
    }

    @Override
    protected Map<String, Object> getParameters() {
        return this.variablesMap;
    }

    @Override
    protected String getGenerateFileName() {
        return "rapport_caisse_quotidien";
    }

    public byte[] export(LocalDate date) {

        List<DailyCashRegisterReportDTO> dailyReports = cashRegisterReportService.getDailyReport(date);

        // Calculate totals
        int totalSales = dailyReports.stream().mapToInt(r -> r.totalSales() != null ? r.totalSales() : 0).sum();
        int totalDiscrepancy = dailyReports
            .stream()
            .filter(DailyCashRegisterReportDTO::isClosed)
            .mapToInt(r -> Math.abs(r.discrepancy() != null ? r.discrepancy() : 0))
            .sum();


        this.getParameters().put("dailyReports", dailyReports);
        this.getParameters().put("reportDate", date);
        this.getParameters().put("totalSales", totalSales);
        this.getParameters().put("totalDiscrepancy", totalDiscrepancy);
        this.getParameters().put("reportTitle", "Rapport de Caisse Quotidien");
        this.getParameters().put("page_count", "1/1");
        super.getCommonParameters();
        return super.exportReportToPdf();
    }
}
