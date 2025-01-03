package com.kobe.warehouse.service.report;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.lowagie.text.DocumentException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Service
@Transactional(readOnly = true)
public class SaleInvoiceReportService extends CommonReportService {

    private static final String SALE = "sale";
    private final SpringTemplateEngine templateEngine;
    private final StorageService storageService;

    private final Map<String, Object> variablesMap = new HashMap<>();
    private String templateFile;

    public SaleInvoiceReportService(
        SpringTemplateEngine templateEngine,
        StorageService storageService,
        FileStorageProperties fileStorageProperties
    ) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
        this.storageService = storageService;
    }

    private void setVariables(SaleDTO dto) {
        templateFile = Constant.INVOICE_TEMPLATE_FILE;
        getParameters().put(Constant.MAGASIN, storageService.getUser().getMagasin());
        getParameters().put(SALE, dto);
    }

    private String buildInvoice() {
        String destFilePath = this.getDestFilePath();
        try {
            try (OutputStream outputStream = new FileOutputStream(destFilePath)) {
                ITextRenderer renderer = new ITextRenderer();
                SharedContext sharedContext = renderer.getSharedContext();
                sharedContext.getTextRenderer().setSmoothingThreshold(0);
                sharedContext.setPrint(true);
                renderer.setDocumentFromString(this.getTemplateAsHtml());
                renderer.layout();
                renderer.createPDF(outputStream);
            }
        } catch (IOException | DocumentException e) {
            log.debug("{0}", e);
        }

        return destFilePath;
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
        return "customerInvoice";
    }

    public Resource printInvoice(SaleDTO dto) throws MalformedURLException {
        setVariables(dto);
        return this.getResource(buildInvoice());
    }
}
