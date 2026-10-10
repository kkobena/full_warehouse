package com.kobe.warehouse.service.stat.impl;

import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.HistoriqueProduitAchatMensuelleWrapper;
import com.kobe.warehouse.service.dto.HistoriqueProduitAchats;
import com.kobe.warehouse.service.dto.HistoriqueProduitAchatsSummary;
import com.kobe.warehouse.service.dto.HistoriqueProduitVente;
import com.kobe.warehouse.service.dto.HistoriqueProduitVenteMensuelleSummary;
import com.kobe.warehouse.service.dto.HistoriqueProduitVenteMensuelleWrapper;
import com.kobe.warehouse.service.dto.HistoriqueProduitVenteSummary;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.dto.produit.HistoriqueProduitInfo;
import com.kobe.warehouse.service.errors.ReportFileExportException;
import com.kobe.warehouse.service.report.CommonReportService;
import com.kobe.warehouse.service.report.Constant;
import com.kobe.warehouse.service.utils.DateUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        StorageService storageService
    ) {
        super( storageService);
        this.templateEngine = templateEngine;
    }

    private byte[] print(
        List<HistoriqueProduitVente> datas,
        HistoriqueProduitVenteSummary historiqueProduitVenteSummary,
        HistoriqueProduitInfo historiqueProduitInfo,
        ReportPeriode reportPeriode
    ) {
        this.fileName = "historique_ventes";
        templateFile = Constant.HISTORIQUE_VENTE_DAILY_ARTICLE_TEMPLATE_FILE;
        getParameters().put(Constant.ITEMS, datas);
        getParameters().put(Constant.REPORT_SUMMARY, historiqueProduitVenteSummary);
        getParameters()
            .put(
                Constant.REPORT_TITLE,
                "Historique des ventes de " +
                historiqueProduitInfo.getLibelle() +
                " [ " +
                historiqueProduitInfo.getCodeCip() +
                " ]" +
                " du " +
                DateUtil.format(reportPeriode.from()) +
                " au " +
                DateUtil.format(reportPeriode.to())
            );

        super.getCommonParameters();
        return super.printOneReceiptPage();
    }

    private byte[] printHistoriquesAchatsMensuel(
        List<HistoriqueProduitAchatMensuelleWrapper> datas,
        HistoriqueProduitAchatsSummary historiqueProduitAchatsSummary,
        HistoriqueProduitInfo historiqueProduitInfo,
        ReportPeriode reportPeriode
    ) {
        this.fileName = "historique_achats";
        this.templateFile = Constant.HISTORIQUE_ACHAT_YEARLY_ARTICLE_TEMPLATE_FILE;
        getParameters().put(Constant.ITEMS, datas);
        getParameters().put(Constant.REPORT_SUMMARY, historiqueProduitAchatsSummary);
        getParameters()
            .put(
                Constant.REPORT_TITLE,
                "Historique des achats de " +
                historiqueProduitInfo.getLibelle() +
                " [ " +
                historiqueProduitInfo.getCodeCip() +
                " ]" +
                " du " +
                DateUtil.format(reportPeriode.from()) +
                " au " +
                DateUtil.format(reportPeriode.to())
            );

        super.getCommonParameters();
        return super.printOneReceiptPage();
    }

    private byte[] printHistoriquesAchats(
        List<HistoriqueProduitAchats> datas,
        HistoriqueProduitAchatsSummary historiqueProduitAchatsSummary,
        HistoriqueProduitInfo historiqueProduitInfo,
        ReportPeriode reportPeriode
    ) {
        this.fileName = "historique_achats";
        templateFile = Constant.HISTORIQUE_ACHAT_DAILY_ARTICLE_TEMPLATE_FILE;
        getParameters().put(Constant.ITEMS, datas);
        getParameters().put(Constant.REPORT_SUMMARY, historiqueProduitAchatsSummary);
        getParameters()
            .put(
                Constant.REPORT_TITLE,
                "Historique des achats de " +
                historiqueProduitInfo.getLibelle() +
                " [ " +
                historiqueProduitInfo.getCodeCip() +
                " ]" +
                " du " +
                DateUtil.format(reportPeriode.from()) +
                " au " +
                DateUtil.format(reportPeriode.to())
            );

        super.getCommonParameters();
        return super.printOneReceiptPage();
    }

    private byte[] printHistoriquesMensuelles(
        List<HistoriqueProduitVenteMensuelleWrapper> datas,
        HistoriqueProduitVenteMensuelleSummary produitVenteMensuelleSummary,
        HistoriqueProduitInfo historiqueProduitInfo,
        ReportPeriode reportPeriode
    ) {
        this.fileName = "historique_ventes";
        templateFile = Constant.HISTORIQUE_VENTE_YEARLY_ARTICLE_TEMPLATE_FILE;
        getParameters().put(Constant.ITEMS, datas);
        getParameters().put(Constant.REPORT_SUMMARY, produitVenteMensuelleSummary);
        getParameters()
            .put(
                Constant.REPORT_TITLE,
                "Historique des ventes de " +
                historiqueProduitInfo.getLibelle() +
                " [ " +
                historiqueProduitInfo.getCodeCip() +
                " ]" +
                " du " +
                DateUtil.format(reportPeriode.from()) +
                " au " +
                DateUtil.format(reportPeriode.to())
            );

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

    public byte[] exportHistoriqueVenteMensuelleToPdf(
        List<HistoriqueProduitVenteMensuelleWrapper> datas,
        HistoriqueProduitVenteMensuelleSummary produitVenteMensuelleSummary,
        HistoriqueProduitInfo historiqueProduitInfo,
        ReportPeriode reportPeriode
    ) throws ReportFileExportException {
        try {
            return printHistoriquesMensuelles(datas, produitVenteMensuelleSummary, historiqueProduitInfo, reportPeriode);
        } catch (Exception e) {
            log.error("exportHistoriqueVenteMensuelleToPdf", e);
            throw new ReportFileExportException();
        }
    }

    public byte[] exportHistoriqueVenteToPdf(
        List<HistoriqueProduitVente> datas,
        HistoriqueProduitVenteSummary historiqueProduitVenteSummary,
        HistoriqueProduitInfo historiqueProduitInfo,
        ReportPeriode reportPeriode
    ) throws ReportFileExportException {
        try {
            return print(datas, historiqueProduitVenteSummary, historiqueProduitInfo, reportPeriode);
        } catch (Exception e) {
            log.error("exportHistoriqueVenteToPdf", e);
            throw new ReportFileExportException();
        }
    }

    public byte[] exportHistoriqueAchatsMensuelToPdf(
        List<HistoriqueProduitAchatMensuelleWrapper> datas,
        HistoriqueProduitAchatsSummary historiqueProduitAchatsSummary,
        HistoriqueProduitInfo historiqueProduitInfo,
        ReportPeriode reportPeriode
    ) throws ReportFileExportException {
        try {
            return printHistoriquesAchatsMensuel(datas, historiqueProduitAchatsSummary, historiqueProduitInfo, reportPeriode);
        } catch (Exception e) {
            log.error("exportHistoriqueAchatsMensuelToPdf", e);
            throw new ReportFileExportException();
        }
    }

    public byte[] exportHistoriqueAchatsToPdf(
        List<HistoriqueProduitAchats> datas,
        HistoriqueProduitAchatsSummary historiqueProduitAchatsSummary,
        HistoriqueProduitInfo historiqueProduitInfo,
        ReportPeriode reportPeriode
    ) throws ReportFileExportException {
        try {
            return printHistoriquesAchats(datas, historiqueProduitAchatsSummary, historiqueProduitInfo, reportPeriode);
        } catch (Exception e) {
            log.error("exportHistoriqueVenteToPdf", e);
            throw new ReportFileExportException();
        }
    }
}
