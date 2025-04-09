package com.kobe.warehouse.service.activity_summary;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.projection.*;
import com.kobe.warehouse.service.dto.records.ActivitySummaryRecord;
import com.kobe.warehouse.service.dto.records.Amount;
import com.kobe.warehouse.service.errors.ReportFileExportException;
import com.kobe.warehouse.service.report.CommonReportService;
import com.kobe.warehouse.service.report.Constant;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class ActivitySummaryReportService extends CommonReportService {

    private final SpringTemplateEngine templateEngine;
    private final StorageService storageService;

    private final Map<String, Object> variablesMap = new HashMap<>();
    private String templateFile;

    public ActivitySummaryReportService(
        SpringTemplateEngine templateEngine,
        StorageService storageService,
        FileStorageProperties fileStorageProperties
    ) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
        this.storageService = storageService;
    }

    @Override
    protected List<?> getItems() {
        return List.of();
    }

    @Override
    protected int getMaxiRowCount() {
        return 0;
    }

    @Override
    protected String getTemplateAsHtml() {
        return templateEngine.process(templateFile, super.getContextVariables());
    }

    @Override
    protected String getTemplateAsHtml(Context context) {
        this.getParameters().forEach(context::setVariable);
        return templateEngine.process(templateFile, context);
    }

    @Override
    protected Map<String, Object> getParameters() {
        return this.variablesMap;
    }

    @Override
    protected String getGenerateFileName() {
        return "activity_summary";
    }

    public String print(ActivitySummaryRecord activitySummaryRecord) {
        Magasin magasin = storageService.getUser().getMagasin();
        templateFile = Constant.ACTIVITY_SUMMARY;
        getParameters().put(Constant.MAGASIN, magasin);
        getParameters().put(Constant.ENTITY, activitySummaryRecord);
        getParameters().put("totalRecette", getTotalRecette(activitySummaryRecord.chiffreAffaire().recettes()));
        getParameters().put("totalMvt", getTotalMouvementCaisse(activitySummaryRecord.chiffreAffaire().mouvementCaisses()));
        getParameters().put("totauxAchat", getTotalAchatTiersPayant(activitySummaryRecord.achatTiersPayants()));
        getParameters().put("totauxReglement", getTotalReglementTiersPayants(activitySummaryRecord.reglementTiersPayants()));
        getParameters().put("totauxGroupeFour", getTotaGroupeFournisseurAchat(activitySummaryRecord.groupeFournisseurAchats()));
        getParameters().put(Constant.FOOTER, "\"" + super.builderFooter(magasin) + "\"");
        return super.printOneReceiptPage(getDestFilePath());
    }

    private BigDecimal getTotalRecette(List<Recette> recettes) {
        return recettes.stream().map(Recette::getMontantReel).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getTotalMouvementCaisse(List<MouvementCaisse> mouvementCaisses) {
        return mouvementCaisses.stream().map(MouvementCaisse::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Amount getTotalAchatTiersPayant(List<AchatTiersPayant> achatTiersPayants) {
        int clientCount = 0;
        BigDecimal montant = BigDecimal.ZERO;
        int bonsCount = 0;
        for (AchatTiersPayant achatTiersPayant : achatTiersPayants) {
            clientCount += achatTiersPayant.getClientCount();
            montant = montant.add(achatTiersPayant.getMontant());
            bonsCount += achatTiersPayant.getBonsCount();
        }
        return new Amount(clientCount, montant, bonsCount);
    }

    private Amount getTotalReglementTiersPayants(List<ReglementTiersPayants> reglementTiersPayants) {
        int montantFacture = 0;
        int montantReglement = 0;
        for (ReglementTiersPayants r : reglementTiersPayants) {
            montantFacture += r.getMontantFacture();
            montantReglement += r.getMontantReglement();
        }
        return new Amount(montantFacture, montantReglement);
    }

    private Amount getTotaGroupeFournisseurAchat(List<GroupeFournisseurAchat> groupeFournisseurAchats) {
        BigDecimal montantTtc = BigDecimal.ZERO;
        BigDecimal montantTva = BigDecimal.ZERO;
        BigDecimal montantHt = BigDecimal.ZERO;
        for (GroupeFournisseurAchat g : groupeFournisseurAchats) {
            montantHt = montantHt.add(g.getMontantHt());
            montantTva = montantTva.add(g.getMontantTva());
            montantTtc = montantTtc.add(g.getMontantTtc());
        }
        return new Amount(montantTtc, montantTva, montantHt);
    }

    public Resource printToPdf(ActivitySummaryRecord activitySummaryRecord) throws ReportFileExportException {
        try {
            return this.getResource(print(activitySummaryRecord));
        } catch (Exception e) {
            log.error("printToPdf", e);
            throw new ReportFileExportException();
        }
    }
}
