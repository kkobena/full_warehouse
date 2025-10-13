package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.errors.FileStorageException;
import com.kobe.warehouse.service.errors.ReportFileExportException;
import com.kobe.warehouse.service.facturation.dto.GroupeFactureDto;
import com.kobe.warehouse.service.report.Constant;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Service
public class GroupeFactureReportServiceImpl implements GroupeFactureReportService {

    private final Logger log = LoggerFactory.getLogger(GroupeFactureReportServiceImpl.class);
    private final FileStorageProperties fileStorageProperties;
    private final SpringTemplateEngine templateEngine;
    private final StorageService storageService;
    private final FacturationReportService facturationReportService;

    private final Map<String, Object> variablesMap = new HashMap<>();

    private final String templateFile;

    public GroupeFactureReportServiceImpl(
        FileStorageProperties fileStorageProperties,
        SpringTemplateEngine templateEngine,
        StorageService storageService,
        FacturationReportService facturationReportService
    ) {
        this.fileStorageProperties = fileStorageProperties;
        this.templateEngine = templateEngine;
        this.storageService = storageService;
        this.facturationReportService = facturationReportService;
        this.templateFile = Constant.FACTURATION_GROUPE_TEMPLATE_FILE;
    }

    private Context getContext() {
        Locale locale = Locale.forLanguageTag("fr");
        return new Context(locale);
    }

    private Context getContextVariables() {
        Context context = getContext();
        this.getParameters().forEach(context::setVariable);
        return context;
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
                "groupe_facture_" +
                codeFacture +
                "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_H_mm_ss")) +
                ".pdf"
            )
            .toFile()
            .getAbsolutePath();
    }

    private String getTemplateAsHtml() {
        return templateEngine.process(templateFile, getContextVariables());
    }

    private Map<String, Object> getParameters() {
        return this.variablesMap;
    }

    private String printOneReceiptPage(String codeFacture) {
        String filePath = getDestFilePath(codeFacture);
        try (OutputStream outputStream = new FileOutputStream(filePath)) {
            ITextRenderer renderer = new ITextRenderer();
            SharedContext sharedContext = renderer.getSharedContext();
            sharedContext.setPrint(true);
            renderer.setDocumentFromString(this.getTemplateAsHtml());
            renderer.layout();
            renderer.createPDF(outputStream);
        } catch (IOException  e) {
            log.debug("printOneReceiptPage", e);
        }
        return filePath;
    }

    private void buildCommonParameters(Magasin magasin) {
        getParameters().put(Constant.MAGASIN, magasin);
        getParameters().put(Constant.FOOTER, "\"" + builderFooter(magasin) + "\"");
    }

    private StringBuilder builderFooter(Magasin magasin) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.isNotEmpty(magasin.getRegistre())) {
            builder.append("RC N° ").append(magasin.getRegistre());
        }
        if (StringUtils.isNotEmpty(magasin.getCompteContribuable())) {
            builder.append(" - CC N° ").append(magasin.getCompteContribuable());
        }
        if (StringUtils.isNotEmpty(magasin.getNumComptable())) {
            builder.append(" - CPT N°: ").append(magasin.getNumComptable());
        }
        if (StringUtils.isNotEmpty(magasin.getPhone())) {
            builder.append("- Tel: ").append(magasin.getPhone());
        }
        if (StringUtils.isNotEmpty(magasin.getAddress())) {
            builder.append("- Adr: ").append(magasin.getAddress());
        }
        return builder;
    }

    private String print(GroupeFactureDto factureTiersPayant) {
        getParameters().put(Constant.ENTITY, factureTiersPayant);

        return printOneReceiptPage(factureTiersPayant.getNumFacture());
    }

    private Resource getResource(String reportPath) throws MalformedURLException {
        return new UrlResource(Paths.get(reportPath).toUri());
    }

    private String mergeDocuments(List<String> pdfFiles, String destFilePath) throws IOException {
        PDFMergerUtility pdfMerger = new PDFMergerUtility();
        pdfMerger.setDestinationFileName(destFilePath);

        // Add the source PDF files
        for (String file : pdfFiles) {
            pdfMerger.addSource(new File(file));
        }
        pdfMerger.mergeDocuments(null);
        return destFilePath;
    }

    @Override
    public Resource printToPdf(List<GroupeFactureDto> factureTiersPayants) throws ReportFileExportException {
        try {
            if (CollectionUtils.isEmpty(factureTiersPayants)) {
                throw new ReportFileExportException();
            }
            Magasin magasin = storageService.getUser().getMagasin();
            buildCommonParameters(magasin);
            List<String> pdfFiles = factureTiersPayants.stream().flatMap(e -> buildDocuments(e).stream()).toList();

            return this.getResource(mergeDocuments(pdfFiles, getDestFilePath("recapitulatif_")));
        } catch (Exception e) {
            log.error("printToPdf", e);
            throw new ReportFileExportException();
        }
    }

    @Override
    public Resource printToPdf(GroupeFactureDto factureTiersPayant) throws ReportFileExportException {
        try {
            Magasin magasin = storageService.getUser().getMagasin();
            buildCommonParameters(magasin);
            return this.getResource(
                    mergeDocuments(
                        buildDocuments(factureTiersPayant),
                        getDestFilePath("recapitulatif_" + factureTiersPayant.getNumFacture())
                    )
                );
        } catch (Exception e) {
            log.error("printToPdf", e);
            throw new ReportFileExportException();
        }
    }

    private List<String> buildDocuments(GroupeFactureDto factureTiersPayant) {
        List<String> pdfFiles = new ArrayList<>();
        pdfFiles.add(print(factureTiersPayant));
        pdfFiles.addAll(this.facturationReportService.print(factureTiersPayant.getFacturesTiersPayants()));
        return pdfFiles;
    }
}
