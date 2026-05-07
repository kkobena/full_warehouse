package com.kobe.warehouse.service.report.pdf;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.CompteFournisseurAPDTO;
import com.kobe.warehouse.service.dto.FournisseurAPSummaryDTO;
import com.kobe.warehouse.service.dto.LigneFournisseurAPDTO;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class AccountsPayableApPdfExportService extends AbstractStatistiqueReportService {

    private final SpringTemplateEngine templateEngine;
    private final Map<String, Object> variablesMap = new HashMap<>();

    public AccountsPayableApPdfExportService(
        FileStorageProperties fileStorageProperties,
        StorageService storageService,
        SpringTemplateEngine templateEngine
    ) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
    }

    @Override
    protected String getTemplateAsHtml() {
        return templateEngine.process("reports/comptes-fournisseurs/main", super.getContextVariables());
    }

    @Override
    protected String getTemplateAsHtml(Context context) {
        this.getParameters().forEach(context::setVariable);
        return templateEngine.process("reports/comptes-fournisseurs/main", context);
    }

    @Override
    protected Map<String, Object> getParameters() {
        return this.variablesMap;
    }

    @Override
    protected String getGenerateFileName() {
        return "comptes_fournisseurs";
    }

    public byte[] exportGlobal(List<CompteFournisseurAPDTO> comptes, FournisseurAPSummaryDTO summary,
                               LocalDate fromDate, LocalDate toDate) {
        variablesMap.clear();
        variablesMap.put("comptes", comptes);
        variablesMap.put("summary", summary);
        variablesMap.put("reportTitle", "État des Comptes Fournisseurs");
        variablesMap.put("periode", buildPeriodeLabel(fromDate, toDate));
        variablesMap.put("totalSolde", comptes.stream().mapToLong(CompteFournisseurAPDTO::solde).sum());
        variablesMap.put("generatedAt", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        super.getCommonParameters();
        return super.exportReportToPdf();
    }

    public byte[] exportFournisseur(CompteFournisseurAPDTO compte, List<LigneFournisseurAPDTO> lignes) {
        variablesMap.clear();
        variablesMap.put("compte", compte);
        variablesMap.put("lignes", lignes);
        variablesMap.put("reportTitle", "Détail Compte Fournisseur — " + compte.fournisseurName());
        variablesMap.put("totalRestant", lignes.stream().mapToLong(LigneFournisseurAPDTO::restantDu).sum());
        variablesMap.put("generatedAt", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        super.getCommonParameters();

        Context ctx = super.getContextVariables();
        return renderTemplate("reports/comptes-fournisseur-detail/main", ctx);
    }

    private byte[] renderTemplate(String templateName, Context context) {
        variablesMap.forEach(context::setVariable);
        String html = templateEngine.process(templateName, context);
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            org.xhtmlrenderer.pdf.ITextRenderer renderer = new org.xhtmlrenderer.pdf.ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF: " + e.getMessage(), e);
        }
    }

    private String buildPeriodeLabel(LocalDate fromDate, LocalDate toDate) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (fromDate != null && toDate != null) {
            return "Du " + fromDate.format(fmt) + " au " + toDate.format(fmt);
        }
        if (fromDate != null) return "À partir du " + fromDate.format(fmt);
        if (toDate != null) return "Jusqu'au " + toDate.format(fmt);
        return "Toutes périodes";
    }
}
