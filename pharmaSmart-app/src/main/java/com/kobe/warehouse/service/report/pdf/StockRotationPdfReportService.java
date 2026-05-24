package com.kobe.warehouse.service.report.pdf;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.enumeration.CategorieABC;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.report.StockRotationDTO;
import com.kobe.warehouse.service.report.StockRotationReportService;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StockRotationPdfReportService extends AbstractStatistiqueReportService {
    private final SpringTemplateEngine templateEngine;
    private final Map<String, Object> variablesMap = new HashMap<>();

    private final StockRotationReportService stockRotationReportService;

    public StockRotationPdfReportService(FileStorageProperties fileStorageProperties, StorageService storageService, SpringTemplateEngine templateEngine, StockRotationReportService stockRotationReportService) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
        this.stockRotationReportService = stockRotationReportService;
    }


    @Override
    protected String getTemplateAsHtml() {
        return templateEngine.process("reports/stock-rotation/main", super.getContextVariables());
    }

    @Override
    protected String getTemplateAsHtml(Context context) {
        this.getParameters().forEach(context::setVariable);
        return templateEngine.process("reports/stock-rotation/main", context);
    }

    @Override
    protected Map<String, Object> getParameters() {
        return this.variablesMap;
    }

    @Override
    protected String getGenerateFileName() {
        return "rapport_rotation_stock";
    }


    public byte[] export() {
        List<StockRotationDTO> rotations = stockRotationReportService.getAllStockRotation();
        Map<CategorieABC, Long> counts = stockRotationReportService.getStockRotationCountByABCClassification();

        this.getParameters().put("rotations", rotations);
        this.getParameters().put("countA", counts.getOrDefault(CategorieABC.A, 0L));
        this.getParameters().put("countB", counts.getOrDefault(CategorieABC.B, 0L));
        this.getParameters().put("countC", counts.getOrDefault(CategorieABC.C, 0L));
        this.getParameters().put("reportTitle", "Rapport de Rotation du Stock (Analyse ABC)");

        this.getParameters().put("page_count", "1/1");
        super.getCommonParameters();

        return super.exportReportToPdf();
    }
}
