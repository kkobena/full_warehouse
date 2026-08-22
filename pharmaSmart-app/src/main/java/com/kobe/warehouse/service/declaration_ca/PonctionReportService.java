package com.kobe.warehouse.service.declaration_ca;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.declaration_ca.dto.ModeCalculPonction;
import com.kobe.warehouse.service.declaration_ca.dto.PonctionDTO;
import com.kobe.warehouse.service.declaration_ca.dto.PonctionLigneDTO;
import com.kobe.warehouse.service.report.CommonReportService;
import com.kobe.warehouse.service.report.Constant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Justificatif PDF d'une ponction.
 */
@Service
public class PonctionReportService extends CommonReportService {

    private final SpringTemplateEngine templateEngine;
    private final StorageService storageService;

    private final Map<String, Object> variablesMap = new HashMap<>();

    private List<PonctionLigneDTO> items;

    public PonctionReportService(
        SpringTemplateEngine templateEngine,
        FileStorageProperties fileStorageProperties,
        StorageService storageService
    ) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
        this.storageService = storageService;
    }

    @Override
    protected List<PonctionLigneDTO> getItems() {
        return items;
    }

    @Override
    protected int getMaxiRowCount() {
        return Constant.PAGE_SIZE;
    }

    @Override
    protected String getTemplateAsHtml() {
        return templateEngine.process(Constant.PONCTION_TEMPLATE_FILE, super.getContextVariables());
    }

    @Override
    protected String getTemplateAsHtml(Context context) {
        this.getParameters().forEach(context::setVariable);
        return templateEngine.process(Constant.PONCTION_TEMPLATE_FILE, context);
    }

    @Override
    protected Map<String, Object> getParameters() {
        return this.variablesMap;
    }

    @Override
    protected String getGenerateFileName() {
        return "justificatif_ponction";
    }

    public byte[] exportToPdfBytes(PonctionDTO ponction, List<PonctionLigneDTO> lignes) {
        this.items = lignes;
        Magasin magasin = storageService.getUser().getMagasin();
        int itemSize = items.size();

        getParameters().put(Constant.MAGASIN, magasin);
        getParameters().put(Constant.REPORT_TITLE, titre(ponction));
        getParameters().put(Constant.REPORT_SUMMARY, ponction);
        getParameters().put("modeCalcul", baseDeCalcul(ponction));
        getParameters().put(Constant.ITEM_SIZE, itemSize);
        getParameters().put(Constant.FOOTER, "\"" + super.builderFooter(magasin) + "\"");

        if (itemSize > Constant.PAGE_SIZE) {
            getParameters().put(Constant.ITEMS, items.subList(0, Constant.PAGE_SIZE));
            getParameters().put(Constant.IS_LAST_PAGE, false);
            return super.exportMultiplePagesToByteArray();
        }
        getParameters().put(Constant.ITEMS, items);
        getParameters().put(Constant.IS_LAST_PAGE, true);
        getParameters().put(Constant.PAGE_COUNT, "1/1");
        return super.exportReportToPdf();
    }

    private String titre(PonctionDTO ponction) {
        return "Justificatif de ponction du " + ponction.dateDebut() + " au " + ponction.dateFin();
    }


    private String baseDeCalcul(PonctionDTO ponction) {
        return ponction.modeCalcul() == ModeCalculPonction.POURCENTAGE
            ? ponction.valeurSaisie() + " % du chiffre d'affaires après exclusions"
            : ponction.valeurSaisie() + " F (montant fixe)";
    }
}
