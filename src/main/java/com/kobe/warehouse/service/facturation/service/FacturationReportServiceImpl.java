package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.Tuple4;
import com.kobe.warehouse.service.errors.FileStorageException;
import com.kobe.warehouse.service.errors.ReportFileExportException;
import com.kobe.warehouse.service.report.CommonReportService;
import com.kobe.warehouse.service.report.Constant;
import com.kobe.warehouse.service.utils.NumberUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class FacturationReportServiceImpl extends CommonReportService implements FacturationReportService {

    private final SpringTemplateEngine templateEngine;
    private final StorageService storageService;
    private final FileStorageProperties fileStorageProperties;

    private final Map<String, Object> variablesMap = new HashMap<>();

    private final String templateFile;

    public FacturationReportServiceImpl(
        FileStorageProperties fileStorageProperties,
        SpringTemplateEngine templateEngine,
        StorageService storageService
    ) {
        super(fileStorageProperties);
        this.templateEngine = templateEngine;
        this.storageService = storageService;
        this.fileStorageProperties = fileStorageProperties;
        this.templateFile = Constant.FACTURATION_TEMPLATE_FILE;
    }

    private String getDestFilePath(String codeFacture) {
        Path fileStorageLocation = Paths.get(fileStorageProperties.getReportsDir()).toAbsolutePath().normalize();

        try {
            Files.createDirectories(fileStorageLocation);
        } catch (IOException ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }

        return fileStorageLocation
            .resolve(
                "facture_" + codeFacture + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_H_mm_ss")) + ".pdf"
            )
            .toFile()
            .getAbsolutePath();
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
    protected String getGenerateFileName() {
        return "facture";
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

    private void buildCommonParameters(Magasin magasin) {
        getParameters().put(Constant.MAGASIN, magasin);
        getParameters().put(Constant.FOOTER, "\"" + super.builderFooter(magasin) + "\"");
    }

    private String print(FactureTiersPayant factureTiersPayant) {
        Tuple4 total = buildSummary(factureTiersPayant);
        getParameters().put(Constant.ENTITY, factureTiersPayant);
        getParameters().put(Constant.FACTURE_TOTAL, total);
        getParameters().put(Constant.FACTURE_TOTAL_LETTERS, NumberUtil.getNumberToWords(total.e2()).toUpperCase());
        return super.printOneReceiptPage(getDestFilePath(factureTiersPayant.getNumFacture()));
    }

    @Override
    public List<String> print(List<FactureTiersPayant> factureTiersPayants) {
        buildCommonParameters(storageService.getUser().getMagasin());
        return factureTiersPayants.stream().map(this::print).toList();
    }

    private Tuple4 buildSummary(FactureTiersPayant factureTiersPayant) {
        long montantVente = 0;
        long montantAttendu = 0;
        long montantRemiseVente = 0;
        long montantRemiseForfaitaire = 0;
        for (ThirdPartySaleLine partySaleLine : factureTiersPayant.getFacturesDetails()) {
            ThirdPartySales thirdPartySales = partySaleLine.getSale();
            montantVente += thirdPartySales.getSalesAmount();
            montantAttendu += partySaleLine.getMontant();
            montantRemiseVente += Objects.requireNonNullElse(thirdPartySales.getDiscountAmount(), 0);
            montantRemiseForfaitaire += Objects.requireNonNullElse(factureTiersPayant.getRemiseForfetaire(), 0L);
        }
        return new Tuple4(montantVente, montantAttendu, montantRemiseVente, montantRemiseForfaitaire);
    }

    @Override
    public Resource printToPdf(List<FactureTiersPayant> factureTiersPayants) throws ReportFileExportException {
        try {
            if (CollectionUtils.isEmpty(factureTiersPayants)) {
                throw new ReportFileExportException();
            }
            Magasin magasin = storageService.getUser().getMagasin();
            buildCommonParameters(magasin);
            return this.getResource(super.mergeDocuments(factureTiersPayants.stream().map(this::print).toList()));
        } catch (Exception e) {
            log.error("printToPdf all", e);
            throw new ReportFileExportException();
        }
    }

    @Override
    public Resource printToPdf(FactureTiersPayant factureTiersPayant) throws ReportFileExportException {
        try {
            Magasin magasin = storageService.getUser().getMagasin();
            buildCommonParameters(magasin);
            return this.getResource(print(factureTiersPayant));
        } catch (Exception e) {
            log.error("printToPdf", e);
            throw new ReportFileExportException();
        }
    }
}
