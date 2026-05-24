package com.kobe.warehouse.service.report.pdf;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.enumeration.StockAlertType;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.report.StockAlertDTO;
import com.kobe.warehouse.service.report.StockAlertReportService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StockAlertPdfReportService extends AbstractStatistiqueReportService {
    private final SpringTemplateEngine templateEngine;
    private final Map<String, Object> variablesMap = new HashMap<>();

    private final StockAlertReportService stockAlertReportService;

    public StockAlertPdfReportService(FileStorageProperties fileStorageProperties, StorageService storageService, SpringTemplateEngine templateEngine, StockAlertReportService stockAlertReportService) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
        this.stockAlertReportService = stockAlertReportService;
    }


    @Override
    protected String getTemplateAsHtml() {
        return templateEngine.process("reports/stock-alerts/main", super.getContextVariables());
    }

    @Override
    protected String getTemplateAsHtml(Context context) {
        this.getParameters().forEach(context::setVariable);
        return templateEngine.process("reports/stock-alerts/main", context);
    }

    @Override
    protected Map<String, Object> getParameters() {
        return this.variablesMap;
    }

    @Override
    protected String getGenerateFileName() {
        return "rapport_alertes_stock";
    }


    public byte[] export(List<StockAlertType> alertTypes) {

        List<StockAlertDTO> alerts = stockAlertReportService.getStockAlerts(alertTypes, Pageable.unpaged()).getContent();
        Map<StockAlertType, Long> alertCounts = stockAlertReportService.getStockAlertsCount();

        this.getParameters().put("alerts", alerts);
        this.getParameters().put("ruptureCount", alertCounts.getOrDefault(StockAlertType.RUPTURE, 0L));
        this.getParameters().put("alerteCount", alertCounts.getOrDefault(StockAlertType.ALERTE, 0L));
        this.getParameters().put("peremptionCount", alertCounts.getOrDefault(StockAlertType.PEREMPTION, 0L));
        this.getParameters().put("reportTitle", "Rapport d'Alertes Stock");
        super.getCommonParameters();

        return super.exportReportToPdf();
    }
}
