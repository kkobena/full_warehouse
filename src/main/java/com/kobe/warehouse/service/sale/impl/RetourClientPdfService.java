package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.report.CommonReportService;
import com.kobe.warehouse.service.report.Constant;
import com.kobe.warehouse.service.sale.RetourClientService;
import com.kobe.warehouse.service.sale.dto.RetourClientDTO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@Transactional(readOnly = true)
public class RetourClientPdfService extends CommonReportService {

    private static final String TEMPLATE = "retour-client/receipt/main";

    private final RetourClientService retourClientService;
    private final SpringTemplateEngine templateEngine;
    private final Map<String, Object> params = new HashMap<>();

    public RetourClientPdfService(
        RetourClientService retourClientService,
        StorageService storageService,
        SpringTemplateEngine templateEngine,
        FileStorageProperties fileStorageProperties
    ) {
        super(fileStorageProperties, storageService);
        this.retourClientService = retourClientService;
        this.templateEngine = templateEngine;
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
    protected Map<String, Object> getParameters() {
        return params;
    }

    @Override
    protected String getTemplateAsHtml() {
        return templateEngine.process(TEMPLATE, super.getContextVariables());
    }

    @Override
    protected String getTemplateAsHtml(Context context) {
        getParameters().forEach(context::setVariable);
        return templateEngine.process(TEMPLATE, context);
    }

    @Override
    protected String getGenerateFileName() {
        return "retour_client";
    }

    public byte[] generatePdf(Integer retourId) {
        params.clear();
        RetourClientDTO retour = retourClientService.findById(retourId);
        params.put("retour", retour);
        params.put(Constant.SIZE, "80mm 297mm");
        getCommonParameters();
        return exportReportToPdf();
    }
}
