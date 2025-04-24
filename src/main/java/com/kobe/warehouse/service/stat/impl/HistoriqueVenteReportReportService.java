package com.kobe.warehouse.service.stat.impl;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.HistoriqueProduitAchatMensuelleWrapper;
import com.kobe.warehouse.service.dto.HistoriqueProduitAchats;
import com.kobe.warehouse.service.dto.HistoriqueProduitVente;
import com.kobe.warehouse.service.dto.HistoriqueProduitVenteMensuelleWrapper;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.dto.produit.HistoriqueProduitInfo;
import com.kobe.warehouse.service.errors.ReportFileExportException;
import com.kobe.warehouse.service.report.CommonReportService;
import com.kobe.warehouse.service.report.Constant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class HistoriqueVenteReportReportService extends CommonReportService {

    private final SpringTemplateEngine templateEngine;
    private final Map<String, Object> variablesMap = new HashMap<>();
    private String templateFile;
    private String fileName;

    public HistoriqueVenteReportReportService(
        SpringTemplateEngine templateEngine,
        FileStorageProperties fileStorageProperties,
        StorageService storageService
    ) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
    }

    private String print(List<HistoriqueProduitVente> datas, HistoriqueProduitInfo historiqueProduitInfo, ReportPeriode reportPeriode) {
        this.fileName = "historique_ventes";
        templateFile = Constant.HISTORIQUE_VENTE_DAILY_ARTICLE_TEMPLATE_FILE;
        getParameters().put(Constant.ITEMS, datas);
        getParameters().put(Constant.ENTITY, historiqueProduitInfo);
        getParameters().put(Constant.PERIODE, reportPeriode);
        super.getCommonParameters();
        return super.printOneReceiptPage();
    }

    private String printHistoriquesAchatsMensuel(
        List<HistoriqueProduitAchatMensuelleWrapper> datas,
        HistoriqueProduitInfo historiqueProduitInfo,
        ReportPeriode reportPeriode
    ) {
        this.fileName = "historique_achats";
        this.templateFile = Constant.HISTORIQUE_ACHAT_YEARLY_ARTICLE_TEMPLATE_FILE;
        getParameters().put(Constant.ITEMS, datas);
        getParameters().put(Constant.ENTITY, historiqueProduitInfo);
        getParameters().put(Constant.PERIODE, reportPeriode);
        super.getCommonParameters();
        return super.printOneReceiptPage();
    }

    private String printHistoriquesAchats(
        List<HistoriqueProduitAchats> datas,
        HistoriqueProduitInfo historiqueProduitInfo,
        ReportPeriode reportPeriode
    ) {
        this.fileName = "historique_achats";
        templateFile = Constant.HISTORIQUE_ACHAT_DAILY_ARTICLE_TEMPLATE_FILE;
        getParameters().put(Constant.ITEMS, datas);
        getParameters().put(Constant.ENTITY, historiqueProduitInfo);
        getParameters().put(Constant.PERIODE, reportPeriode);
        super.getCommonParameters();
        return super.printOneReceiptPage();
    }

    private String printHistoriquesMensuelles(
        List<HistoriqueProduitVenteMensuelleWrapper> datas,
        HistoriqueProduitInfo historiqueProduitInfo,
        ReportPeriode reportPeriode
    ) {
        this.fileName = "historique_ventes";
        templateFile = Constant.HISTORIQUE_VENTE_YEARLY_ARTICLE_TEMPLATE_FILE;
        getParameters().put(Constant.ITEMS, datas);
        getParameters().put(Constant.ENTITY, historiqueProduitInfo);
        getParameters().put(Constant.PERIODE, reportPeriode);
        super.getCommonParameters();
        return super.printOneReceiptPage();
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
        return fileName;
    }

    public Resource exportHistoriqueVenteMensuelleToPdf(
        List<HistoriqueProduitVenteMensuelleWrapper> datas,
        HistoriqueProduitInfo historiqueProduitInfo,
        ReportPeriode reportPeriode
    ) throws ReportFileExportException {
        try {
            return this.getResource(printHistoriquesMensuelles(datas, historiqueProduitInfo, reportPeriode));
        } catch (Exception e) {
            log.error("exportHistoriqueVenteMensuelleToPdf", e);
            throw new ReportFileExportException();
        }
    }

    public Resource exportHistoriqueVenteToPdf(
        List<HistoriqueProduitVente> datas,
        HistoriqueProduitInfo historiqueProduitInfo,
        ReportPeriode reportPeriode
    ) throws ReportFileExportException {
        try {
            return this.getResource(print(datas, historiqueProduitInfo, reportPeriode));
        } catch (Exception e) {
            log.error("exportHistoriqueVenteToPdf", e);
            throw new ReportFileExportException();
        }
    }

    public Resource exportHistoriqueAchatsMensuelToPdf(
        List<HistoriqueProduitAchatMensuelleWrapper> datas,
        HistoriqueProduitInfo historiqueProduitInfo,
        ReportPeriode reportPeriode
    ) throws ReportFileExportException {
        try {
            return this.getResource(printHistoriquesAchatsMensuel(datas, historiqueProduitInfo, reportPeriode));
        } catch (Exception e) {
            log.error("exportHistoriqueVenteMensuelleToPdf", e);
            throw new ReportFileExportException();
        }
    }

    public Resource exportHistoriqueAchatsToPdf(
        List<HistoriqueProduitAchats> datas,
        HistoriqueProduitInfo historiqueProduitInfo,
        ReportPeriode reportPeriode
    ) throws ReportFileExportException {
        try {
            return this.getResource(printHistoriquesAchats(datas, historiqueProduitInfo, reportPeriode));
        } catch (Exception e) {
            log.error("exportHistoriqueVenteToPdf", e);
            throw new ReportFileExportException();
        }
    }
}
