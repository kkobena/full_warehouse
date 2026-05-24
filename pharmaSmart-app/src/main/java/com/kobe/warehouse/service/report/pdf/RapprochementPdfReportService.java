package com.kobe.warehouse.service.report.pdf;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.facturation.dto.EtatRapprochementDto;
import com.kobe.warehouse.service.facturation.dto.RapprochementParams;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class RapprochementPdfReportService extends AbstractStatistiqueReportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SpringTemplateEngine templateEngine;
    private final Map<String, Object> variablesMap = new HashMap<>();

    public RapprochementPdfReportService(
        FileStorageProperties fileStorageProperties,
        StorageService storageService,
        SpringTemplateEngine templateEngine
    ) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
    }

    @Override
    protected String getTemplateAsHtml() {
        return templateEngine.process("reports/rapprochement/main", super.getContextVariables());
    }

    @Override
    protected String getTemplateAsHtml(Context context) {
        this.getParameters().forEach(context::setVariable);
        return templateEngine.process("reports/rapprochement/main", context);
    }

    @Override
    protected Map<String, Object> getParameters() {
        return this.variablesMap;
    }

    @Override
    protected String getGenerateFileName() {
        return "Etat_Rapprochement";
    }

    public byte[] export(List<EtatRapprochementDto> etats, RapprochementParams params) {
        variablesMap.clear();
        super.getCommonParameters();

        String periode = buildPeriodeLabel(params);
        variablesMap.put("reportTitle", "État de Rapprochement");
        variablesMap.put("periode", periode);
        variablesMap.put("etats", etats);

        // KPI globaux
        variablesMap.put("totalFacture", etats.stream()
            .map(EtatRapprochementDto::totalFacture)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        variablesMap.put("totalRegle", etats.stream()
            .map(EtatRapprochementDto::totalRegle)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        variablesMap.put("ecartTotal", etats.stream()
            .map(EtatRapprochementDto::ecartTotal)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));

        return super.exportReportToPdf();
    }

    private String buildPeriodeLabel(RapprochementParams params) {
        if (params.startDate() != null && params.endDate() != null) {
            return "du " + params.startDate().format(DATE_FMT) + " au " + params.endDate().format(DATE_FMT);
        }
        if (params.startDate() != null) return "à partir du " + params.startDate().format(DATE_FMT);
        if (params.endDate() != null) return "jusqu'au " + params.endDate().format(DATE_FMT);
        return "";
    }
}
