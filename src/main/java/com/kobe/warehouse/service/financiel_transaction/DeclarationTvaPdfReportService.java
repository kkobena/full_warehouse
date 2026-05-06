package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.financiel_transaction.dto.DeclarationTvaLineDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TaxeDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TaxeWrapperDTO;
import com.kobe.warehouse.service.report.Constant;
import com.kobe.warehouse.service.report.pdf.AbstractStatistiqueReportService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class DeclarationTvaPdfReportService extends AbstractStatistiqueReportService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SpringTemplateEngine templateEngine;
    private final StorageService storageService;
    private final Map<String, Object> params = new HashMap<>();

    public DeclarationTvaPdfReportService(
        FileStorageProperties fileStorageProperties,
        StorageService storageService,
        SpringTemplateEngine templateEngine
    ) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
        this.storageService = storageService;
    }

    @Override
    protected String getTemplateAsHtml() {
        return templateEngine.process(Constant.DECLARATION_TVA_TEMPLATE_FILE, super.getContextVariables());
    }

    @Override
    protected String getTemplateAsHtml(Context context) {
        params.forEach(context::setVariable);
        return templateEngine.process(Constant.DECLARATION_TVA_TEMPLATE_FILE, context);
    }

    @Override
    protected Map<String, Object> getParameters() {
        return params;
    }

    @Override
    protected String getGenerateFileName() {
        return "declaration_tva";
    }

    public byte[] export(TaxeWrapperDTO declaration, LocalDate from, LocalDate to) {
        Magasin magasin = storageService.getUser().getMagasin();
        List<DeclarationTvaLineDTO> lines = buildLines(declaration);

        params.clear();
        params.put(Constant.MAGASIN, magasin);
        params.put(Constant.REPORT_TITLE, "Déclaration TVA — " + from.format(FMT) + " au " + to.format(FMT));
        params.put(Constant.FOOTER, "\"" + super.builderFooter(magasin) + "\"");
        params.put("taxes", lines);
        params.put("declaration", new DeclarationSummary(
            declaration.getMontantHt(),
            declaration.getMontantTaxe(),
            declaration.getTvaDeductible(),
            declaration.getTvaNette(),
            declaration.getMontantTtc(),
            declaration.getMontantRemise()
        ));

        return super.exportReportToPdf();
    }

    private List<DeclarationTvaLineDTO> buildLines(TaxeWrapperDTO declaration) {
        return declaration.getTaxes().stream().map(this::toLine).toList();
    }

    private DeclarationTvaLineDTO toLine(TaxeDTO t) {
        int taux = t.getCodeTva() != null ? t.getCodeTva() : 0;
        long baseHtVentes = t.getMontantHt() != null ? t.getMontantHt() : 0L;
        long tvaCollectee = t.getMontantTaxe() != null ? t.getMontantTaxe() : 0L;
        long baseHtAchats = t.getMontantAchat() != null ? t.getMontantAchat() : 0L;
        long tvaDeductible = Math.round(baseHtAchats * taux / 100.0);
        String label = taux == 0 ? "Exonéré" : "TVA " + taux + "%";
        return new DeclarationTvaLineDTO(taux, label, baseHtVentes, tvaCollectee, baseHtAchats, tvaDeductible);
    }

    public record DeclarationSummary(
        long montantHt,
        long tvaCollectee,
        long tvaDeductible,
        long tvaNette,
        long montantTtc,
        long remises
    ) {}
}
