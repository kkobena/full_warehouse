package com.kobe.warehouse.service.reglement.service;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.errors.ReportFileExportException;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentDTO;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentWrapper;
import com.kobe.warehouse.service.report.CommonReportService;
import com.kobe.warehouse.service.report.Constant;
import com.kobe.warehouse.service.utils.NumberUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class ReglementReportServiceImpl extends CommonReportService implements ReglementReportService {

    private final SpringTemplateEngine templateEngine;

    private final Map<String, Object> variablesMap = new HashMap<>();

    private String templateFile;

    public ReglementReportServiceImpl(
        FileStorageProperties fileStorageProperties,
        SpringTemplateEngine templateEngine,
        StorageService storageService
    ) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
    }

    @Override
    public Resource printToPdf(List<InvoicePaymentWrapper> invoicePaymentWrappers) throws ReportFileExportException {
        this.templateFile = Constant.REGLEMENT_GROUP_TEMPLATE_FILE;
        try {
            return this.getResource(print(invoicePaymentWrappers));
        } catch (Exception e) {
            log.error("printToPdf", e);
            throw new ReportFileExportException();
        }
    }

    @Override
    public Resource printToPdf(InvoicePaymentWrapper invoicePayment) throws ReportFileExportException {
        this.templateFile = Constant.REGLEMENT_SINGLE_TEMPLATE_FILE;
        try {
            return this.getResource(print(invoicePayment));
        } catch (Exception e) {
            log.error("printToPdf", e);
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
        return "releve_reglement";
    }

    private String print(InvoicePaymentWrapper invoicePayment) {
        int itemSize = 0;
        int totalAmount = 0;
        for (InvoicePaymentDTO i : invoicePayment.getInvoicePayments()) {
            itemSize += i.getInvoicePaymentItemsCount();
            totalAmount += i.getTotalAmount();
        }
        this.getParameters().put(Constant.REGLEMENT_COUNT, NumberUtil.formatToString(itemSize));
        this.getParameters().put(Constant.REGLEMENT_PAID_AMOUNT, NumberUtil.formatToString(totalAmount));
        this.getParameters().put(Constant.ENTITY, invoicePayment);
        super.getCommonParameters();
        return super.printOneReceiptPage(getDestFilePath());
    }

    private String print(List<InvoicePaymentWrapper> invoicePaymentWrappers) {
        int itemSize = 0;
        int totalAmount = 0;
        for (InvoicePaymentWrapper invoicePaymentWrapper : invoicePaymentWrappers) {
            itemSize += Integer.parseInt(invoicePaymentWrapper.getInvoicePaymentItemsCount().replaceAll(" ", ""));
            totalAmount += Integer.parseInt(invoicePaymentWrapper.getTotalAmount().replaceAll(" ", ""));
        }
        this.getParameters().put(Constant.REGLEMENT_COUNT, NumberUtil.formatToString(itemSize));
        this.getParameters().put(Constant.ITEMS, invoicePaymentWrappers);
        this.getParameters().put(Constant.REGLEMENT_PAID_AMOUNT, NumberUtil.formatToString(totalAmount));
        this.getParameters().put(Constant.REGLEMENT_PERIODE, invoicePaymentWrappers.getFirst().getPeriode());

        super.getCommonParameters();
        return super.printOneReceiptPage(getDestFilePath());
    }
}
