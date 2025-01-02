package com.kobe.warehouse.service.reglement.service;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.receipt.AbstractReceiptServiceImpl;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentReceiptDTO;
import com.kobe.warehouse.service.report.Constant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class ReglementReportServiceImpl extends AbstractReceiptServiceImpl implements ReglementReportService {

    private final Map<String, Object> parametres = new HashMap<>();
    private String path;

    protected ReglementReportServiceImpl(
        SpringTemplateEngine templateEngine,
        StorageService storageService,
        FileStorageProperties fileStorageProperties
    ) {
        super(templateEngine, storageService, fileStorageProperties);
    }

    @Override
    public void printRecipt(InvoicePaymentReceiptDTO invoicePaymentReceipt) {
        this.path = super.fileStorageLocation
            .resolve(
                "reglement_fature_" +
                invoicePaymentReceipt.getCodeFacture() +
                "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_H_mm_ss")) +
                ".pdf"
            )
            .toFile()
            .getAbsolutePath();
        this.parametres.put(Constant.ENTITY, invoicePaymentReceipt);
        super.print();
    }

    @Override
    public Map<String, Object> getParameters() {
        return this.parametres;
    }

    @Override
    public String getTemplate() {
        return Constant.REGLEMENT_TEMPLATE_FILE;
    }

    @Override
    public String getPath() {
        return this.path;
    }
}
