package com.kobe.warehouse.service.reglement.differe.service;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.errors.ReportFileExportException;
import com.kobe.warehouse.service.reglement.differe.dto.*;
import com.kobe.warehouse.service.report.CommonReportService;
import com.kobe.warehouse.service.report.Constant;
import com.kobe.warehouse.service.utils.DateUtil;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReglementDiffereReportServiceImpl extends CommonReportService implements ReglementDiffereReportService {
    private final SpringTemplateEngine templateEngine;
    private final Map<String, Object> variablesMap = new HashMap<>();
    private String templateFile;
    private String fileName;

    public ReglementDiffereReportServiceImpl(FileStorageProperties fileStorageProperties,  SpringTemplateEngine templateEngine,StorageService storageService) {
        super(fileStorageProperties, storageService);  this.templateEngine = templateEngine;
    }

    @Override
    public Resource printListToPdf(List<DiffereDTO> differe, DiffereSummary differeSummary, ReportPeriode reportPeriode) throws ReportFileExportException {
        this.templateFile = Constant.LIST_DIFFERE_PDF_TEMPLATE_FILE;
        this.fileName="liste_des_differes";
        try {
            return this.getResource(print(differe, differeSummary, reportPeriode));
        } catch (Exception e) {
            log.error("printListToPdf", e);
            throw new ReportFileExportException();
        }
    }

    @Override
    public Resource printReglementToPdf(List<ReglementDiffereWrapperDTO> reglements, DifferePaymentSummary differePaymentSummary, ReportPeriode reportPeriode) throws ReportFileExportException {
        this.fileName="liste_des_reglements_differes";
        this.templateFile = Constant.REGLEMENT_DIFFERE_PDF_TEMPLATE_FILE;
        try {
            return this.getResource(printReglement(reglements, differePaymentSummary, reportPeriode));
        } catch (Exception e) {
            log.error("printReglementToPdf", e);
            throw new ReportFileExportException();
        }
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
    private String print(List<DiffereDTO> differe, DiffereSummary differeSummary,ReportPeriode reportPeriode) {
        this.getParameters().put(Constant.ITEMS, differe);
        this.getParameters().put(Constant.REPORT_TITLE, "Liste des différés du " + DateUtil.format(reportPeriode.from() ) + " au " + DateUtil.format(reportPeriode.to()));
        this.getParameters().put(Constant.REPORT_SUMMARY, differeSummary);
        super.getCommonParameters();
        return super.printOneReceiptPage(getDestFilePath());
    }

    private String printReglement(List<ReglementDiffereWrapperDTO> reglements, DifferePaymentSummary differePaymentSummary, ReportPeriode reportPeriode) {
        this.getParameters().put(Constant.ITEMS, reglements);
        this.getParameters().put(Constant.REPORT_TITLE, "Liste des règlements différés du " + DateUtil.format(reportPeriode.from() ) + " au " + DateUtil.format(reportPeriode.to()));
        this.getParameters().put(Constant.REPORT_SUMMARY, differePaymentSummary);

        super.getCommonParameters();
        return super.printOneReceiptPage(getDestFilePath());
    }
}
