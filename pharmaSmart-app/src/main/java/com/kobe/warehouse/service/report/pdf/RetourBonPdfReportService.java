package com.kobe.warehouse.service.report.pdf;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.RetourBonDTO;
import com.kobe.warehouse.service.dto.RetourBonItemDTO;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Service
public class RetourBonPdfReportService extends AbstractStatistiqueReportService {

    private static final String TEMPLATE = "retour/main";
    private static final String RETOUR_BON = "retourBon";
    private static final String RETOUR_BON_ITEMS = "retourBonItems";

    private final SpringTemplateEngine templateEngine;
    private final Map<String, Object> variablesMap = new HashMap<>();

    public RetourBonPdfReportService(
        FileStorageProperties fileStorageProperties,
        StorageService storageService,
        SpringTemplateEngine templateEngine
    ) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
    }

    public byte[] export(RetourBonDTO retourBon) {
        this.variablesMap.clear();
        List<RetourBonItemDTO> items = retourBon.getRetourBonItems()
            .stream()
            .sorted(Comparator.comparing(RetourBonItemDTO::getProduitLibelle, Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

        getParameters().put(RETOUR_BON, retourBon);
        getParameters().put(RETOUR_BON_ITEMS, items);
        getParameters().put("item_size", items.size());
        getParameters().put("isLastPage", true);
        getParameters().put("page_count", "1/1");
        getParameters().put("devise", "CFA");
        String title = retourBon.getReference() != null
            ? "BON DE RETOUR FOURNISSEUR " + retourBon.getReference()
            : "BON DE RETOUR FOURNISSEUR N° " + retourBon.getId();
        getParameters().put("reportTitle", title);
        super.getCommonParameters();
        return super.exportReportToPdf();
    }

    @Override
    protected String getTemplateAsHtml() {
        return templateEngine.process(TEMPLATE, super.getContextVariables());
    }

    @Override
    protected String getTemplateAsHtml(Context context) {
        this.getParameters().forEach(context::setVariable);
        return templateEngine.process(TEMPLATE, context);
    }

    @Override
    protected Map<String, Object> getParameters() {
        return this.variablesMap;
    }

    public byte[] exportGroupe(List<RetourBonDTO> retourBons) {
        this.variablesMap.clear();
        String fournisseurLibelle = retourBons.isEmpty() ? "" : retourBons.getFirst().getFournisseurLibelle();
        long montantTotal = retourBons.stream().mapToLong(RetourBonDTO::getMontantTotal).sum();

        getParameters().put("retourBons", retourBons);
        getParameters().put("fournisseurLibelle", fournisseurLibelle);
        getParameters().put("montantTotalGroupe", montantTotal);
        getParameters().put("dateEdition", LocalDate.now());
        getParameters().put("devise", "CFA");
        getParameters().put("reportTitle", "BORDEREAU GROUPÉ RETOURS FOURNISSEUR — " + fournisseurLibelle);
        super.getCommonParameters();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            Context context = super.getContextVariables();
            this.getParameters().forEach(context::setVariable);
            renderer.setDocumentFromString(templateEngine.process("retour/groupe", context));
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating grouped PDF: " + e.getMessage(), e);
        }
    }

    @Override
    protected String getGenerateFileName() {
        return "retour-bon";
    }
}
