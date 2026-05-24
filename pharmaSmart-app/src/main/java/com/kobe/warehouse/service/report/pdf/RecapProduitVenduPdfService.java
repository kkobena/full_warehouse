package com.kobe.warehouse.service.report.pdf;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.stock.dto.RecapProduitVendu;
import com.kobe.warehouse.service.stock.dto.RecapProduitVenduRequestParam;
import com.kobe.warehouse.service.stock.dto.RecapProduitVenduSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class RecapProduitVenduPdfService extends AbstractStatistiqueReportService {
    private final SpringTemplateEngine templateEngine;
    private final Map<String, Object> variablesMap = new HashMap<>();

    public RecapProduitVenduPdfService(FileStorageProperties fileStorageProperties,
                                       StorageService storageService,
                                       SpringTemplateEngine templateEngine) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
    }

    @Override
    protected String getTemplateAsHtml() {
        return templateEngine.process("reports/recap-produit-vendu/main", super.getContextVariables());
    }

    @Override
    protected String getTemplateAsHtml(Context context) {
        this.getParameters().forEach(context::setVariable);
        return templateEngine.process("reports/recap-produit-vendu/main", context);
    }

    @Override
    protected Map<String, Object> getParameters() {
        return this.variablesMap;
    }

    @Override
    protected String getGenerateFileName() {
        return "recap_produit_vendu";
    }

    public byte[] export(Page<RecapProduitVendu> data,
                        RecapProduitVenduSummary summary,
                        RecapProduitVenduRequestParam requestParam) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        String title = "Récapitulatif des Produits Vendus";
        if (requestParam.startDate() != null && requestParam.endDate() != null) {
            title += " du " + requestParam.startDate().format(formatter) +
                     " au " + requestParam.endDate().format(formatter);
        }

        this.getParameters().put("items", data.getContent());
        this.getParameters().put("reportTitle", title);
        this.getParameters().put("summary", summary);
        this.getParameters().put("totalProducts", summary.totalProducts());
        this.getParameters().put("quantitySold", summary.quantitySold());
        this.getParameters().put("quantityAvoir", summary.quantityAvoir());
        this.getParameters().put("totalSalesAmount", summary.totalSalesAmount());
        this.getParameters().put("totalPurchaseAmount", summary.totalPurchaseAmount());
        this.getParameters().put("totalStock", summary.totalStock());

        super.getCommonParameters();

        return super.exportReportToPdf();
    }
}
