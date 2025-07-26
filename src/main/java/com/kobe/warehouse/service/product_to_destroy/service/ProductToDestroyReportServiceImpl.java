package com.kobe.warehouse.service.product_to_destroy.service;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroyDTO;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroySumDTO;
import com.kobe.warehouse.service.report.CommonReportService;
import com.kobe.warehouse.service.report.Constant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class ProductToDestroyReportServiceImpl extends CommonReportService implements ProductToDestroyReportService {

    private final SpringTemplateEngine templateEngine;
    private final Map<String, Object> variablesMap = new HashMap<>();

    public ProductToDestroyReportServiceImpl(
        FileStorageProperties fileStorageProperties,
        SpringTemplateEngine templateEngine,
        StorageService storageService
    ) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
    }

    @Override
    public ResponseEntity<byte[]> generatePdf(
        List<ProductToDestroyDTO> productToDestroys,
        ProductToDestroySumDTO sum,
        LocalDate from,
        LocalDate to
    ) {
        setTitle("Liste des produits à détruire du ", buildPeriode(from, Objects.requireNonNullElse(to, LocalDate.now())));
        this.getParameters().put(Constant.ITEMS, productToDestroys);
        this.getParameters().put(Constant.REPORT_SUMMARY, sum);
        getCommonParameters();
        return super.genererPdf();
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
        return templateEngine.process(Constant.PERIMES_A_DETRUIRE_TEMPLATE_FILE, super.getContextVariables());
    }

    @Override
    protected String getTemplateAsHtml(Context context) {
        this.getParameters().forEach(context::setVariable);
        return templateEngine.process(Constant.PERIMES_A_DETRUIRE_TEMPLATE_FILE, context);
    }

    @Override
    protected Map<String, Object> getParameters() {
        return this.variablesMap;
    }

    @Override
    protected String getGenerateFileName() {
        return "produits_a_detruire_";
    }
}
