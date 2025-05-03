package com.kobe.warehouse.service.reglement.differe.service;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.receipt.AbstractReceiptServiceImpl;
import com.kobe.warehouse.service.reglement.differe.dto.ReglementDiffereReceiptDTO;
import com.kobe.warehouse.service.report.Constant;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReglementDiffereReceiptServiceImpl extends AbstractReceiptServiceImpl implements ReglementDiffereReceiptService {

    private final Map<String, Object> parametres = new HashMap<>();
    private String path;
    public ReglementDiffereReceiptServiceImpl(SpringTemplateEngine templateEngine, StorageService storageService, FileStorageProperties fileStorageProperties) {
        super(templateEngine, storageService, fileStorageProperties);
    }

    @Override
    protected Map<String, Object> getParameters() {
        return this.parametres;
    }

    @Override
    protected String getTemplate() {
        return Constant.REGLEMENT_DIFFERE_RECEIPT_TEMPLATE_FILE;
    }

    @Override
    protected String getPath() {
        return this.path;
    }

    @Override
    public void printRecipt(ReglementDiffereReceiptDTO dto) {
        this.path = super.fileStorageLocation
            .resolve(
                "reglement_differe_" +
                    "_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_H_mm_ss")) +
                    ".pdf"
            )
            .toFile()
            .getAbsolutePath();
        this.parametres.put(Constant.ENTITY, dto);
        super.print();
    }
}
