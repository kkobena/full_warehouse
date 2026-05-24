package com.kobe.warehouse.service.report.pdf;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.reassort.dto.RepartionSearchQueryDto;
import com.kobe.warehouse.service.reassort.dto.RepartitionStockProduitDto;
import com.kobe.warehouse.service.utils.DateUtil;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RepartitionStockPdfReportService extends AbstractStatistiqueReportService {
    private final SpringTemplateEngine templateEngine;
    private final Map<String, Object> variablesMap = new HashMap<>();

    public RepartitionStockPdfReportService(FileStorageProperties fileStorageProperties, StorageService storageService, SpringTemplateEngine templateEngine) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
    }

    @Override
    protected String getTemplateAsHtml() {
        return templateEngine.process("reports/repartition-stock/main", super.getContextVariables());
    }

    @Override
    protected String getTemplateAsHtml(Context context) {
        this.getParameters().forEach(context::setVariable);
        return templateEngine.process("reports/repartition-stock/main", context);
    }

    @Override
    protected Map<String, Object> getParameters() {
        return this.variablesMap;
    }

    @Override
    protected String getGenerateFileName() {
        return "Repartition_Stock";
    }

    public byte[] export(List<RepartitionStockProduitDto> repartitions, RepartionSearchQueryDto searchQueryDto) {
        this.getParameters().put("repartitions", repartitions);
        this.getParameters().put("reportTitle", buildReportTitle(searchQueryDto));
        this.getParameters().put("totalMouvements", repartitions.size());
        super.getCommonParameters();

        return super.exportReportToPdf();
    }

    private String buildReportTitle(RepartionSearchQueryDto searchQueryDto) {
        StringBuilder title = new StringBuilder("Historique des Répartitions de Stock");
        if (searchQueryDto.dateDebut() != null && searchQueryDto.dateFin() != null) {
            title.append(" du ")
                .append(DateUtil.format(searchQueryDto.dateDebut()))
                .append(" au ")
                .append(DateUtil.format(searchQueryDto.dateFin()));
        }
        return title.toString();
    }
}
