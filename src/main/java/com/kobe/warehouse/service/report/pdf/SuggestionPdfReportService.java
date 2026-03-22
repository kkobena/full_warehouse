package com.kobe.warehouse.service.report.pdf;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.SuggestionLineDTO;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class SuggestionPdfReportService extends AbstractStatistiqueReportService {

    private final SpringTemplateEngine templateEngine;
    private final Map<String, Object> variablesMap = new HashMap<>();

    public SuggestionPdfReportService(
        FileStorageProperties fileStorageProperties,
        StorageService storageService,
        SpringTemplateEngine templateEngine
    ) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
    }

    @Override
    protected String getTemplateAsHtml() {
        return templateEngine.process("reports/suggestion/main", super.getContextVariables());
    }

    @Override
    protected String getTemplateAsHtml(Context context) {
        this.getParameters().forEach(context::setVariable);
        return templateEngine.process("reports/suggestion/main", context);
    }

    @Override
    protected Map<String, Object> getParameters() {
        return this.variablesMap;
    }

    @Override
    protected String getGenerateFileName() {
        return "suggestion_commande";
    }

    public byte[] export(String suggestionReference, String fournisseurLibelle, List<SuggestionLineDTO> lignes) {
        long totalQuantite = lignes.stream().mapToLong(SuggestionLineDTO::quantity).sum();
        long totalMontant = lignes.stream().mapToLong(l -> (long) l.quantity() * l.prixAchat()).sum();

        variablesMap.put("items", lignes);
        variablesMap.put("suggestionReference", suggestionReference);
        variablesMap.put("fournisseurLibelle", fournisseurLibelle);
        variablesMap.put("dateGeneration", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        variablesMap.put("totalQuantite", totalQuantite);
        variablesMap.put("totalMontant", totalMontant);
        variablesMap.put("reportTitle", "Suggestion de commande — " + fournisseurLibelle);

        super.getCommonParameters();

        return super.exportReportToPdf();
    }
}
