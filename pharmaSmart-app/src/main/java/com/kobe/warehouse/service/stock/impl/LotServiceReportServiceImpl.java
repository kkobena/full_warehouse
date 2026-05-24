package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.report.CommonReportService;
import com.kobe.warehouse.service.report.Constant;
import com.kobe.warehouse.service.stock.LotServiceReportService;
import com.kobe.warehouse.service.stock.dto.LotPerimeDTO;
import com.kobe.warehouse.service.stock.dto.LotPerimeValeurSum;
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
public class LotServiceReportServiceImpl extends CommonReportService implements LotServiceReportService {

    private final SpringTemplateEngine templateEngine;
    private final Map<String, Object> variablesMap = new HashMap<>();

    public LotServiceReportServiceImpl(
        SpringTemplateEngine templateEngine,
        FileStorageProperties fileStorageProperties,
        StorageService storageService
    ) {
        super(fileStorageProperties, storageService);
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
    protected String getTemplateAsHtml() {
        return templateEngine.process(Constant.PERIMES_TEMPLATE_FILE, super.getContextVariables());
    }

    @Override
    protected String getTemplateAsHtml(Context context) {
        this.getParameters().forEach(context::setVariable);
        return templateEngine.process(Constant.PERIMES_TEMPLATE_FILE, context);
    }

    @Override
    protected Map<String, Object> getParameters() {
        return this.variablesMap;
    }

    @Override
    protected String getGenerateFileName() {
        return "produits_perimes_";
    }

    @Override
    public ResponseEntity<byte[]> generatePdf(List<LotPerimeDTO> lotPerimes, LotPerimeValeurSum sum, LocalDate from, LocalDate to) {
        setTitle("Liste des produits périmés du ", buildPeriode(from, Objects.requireNonNullElse(to, LocalDate.now())));
        this.getParameters().put(Constant.ITEMS, lotPerimes);
        this.getParameters().put(Constant.REPORT_SUMMARY, sum);
        getCommonParameters();
        return super.genererPdf();
    }
}
