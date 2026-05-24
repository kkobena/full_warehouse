package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.facturation.dto.GroupeFactureDto;
import com.kobe.warehouse.service.report.Constant;
import com.kobe.warehouse.service.report.pdf.AbstractStatistiqueReportService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class GroupeFacturePdfExportServiceImpl extends AbstractStatistiqueReportService implements GroupeFacturePdfExportService {

    private final SpringTemplateEngine templateEngine;
    private final FacturationPdfExportService facturationPdfExportService;
    private final Map<String, Object> variablesMap = new HashMap<>();
    private static final String TEMPLATE_FILE = Constant.FACTURATION_GROUPE_TEMPLATE_FILE;

    public GroupeFacturePdfExportServiceImpl(
        FileStorageProperties fileStorageProperties,
        SpringTemplateEngine templateEngine,
        StorageService storageService,
        FacturationPdfExportService facturationPdfExportService
    ) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
        this.facturationPdfExportService = facturationPdfExportService;
    }

    @Override
    protected String getTemplateAsHtml() {
        return templateEngine.process(TEMPLATE_FILE, super.getContextVariables());
    }

    @Override
    protected String getTemplateAsHtml(Context context) {
        this.getParameters().forEach(context::setVariable);
        return templateEngine.process(TEMPLATE_FILE, context);
    }

    @Override
    protected Map<String, Object> getParameters() {
        return this.variablesMap;
    }

    @Override
    protected String getGenerateFileName() {
        return "groupe_facture";
    }

    private byte[] generateGroupePdf(GroupeFactureDto groupeFacture) {
        getParameters().put(Constant.ENTITY, groupeFacture);
        return super.exportReportToPdf();
    }

    private List<byte[]> buildDocuments(GroupeFactureDto groupeFacture) {
        List<byte[]> pdfBytes = new ArrayList<>();
        pdfBytes.add(generateGroupePdf(groupeFacture));
        pdfBytes.addAll(facturationPdfExportService.exportAll(groupeFacture.getFacturesTiersPayants()));
        return pdfBytes;
    }

    @Override
    public byte[] exportToPdf(GroupeFactureDto groupeFacture) {
        super.getCommonParameters();
        return mergePdfBytes(buildDocuments(groupeFacture));
    }

    @Override
    public byte[] exportToPdf(List<GroupeFactureDto> groupeFactures) {
        if (CollectionUtils.isEmpty(groupeFactures)) {
            throw new IllegalArgumentException("La liste des groupes de factures ne peut pas être vide");
        }
        super.getCommonParameters();
        List<byte[]> allPdfBytes = groupeFactures.stream().flatMap(gf -> buildDocuments(gf).stream()).toList();
        return mergePdfBytes(allPdfBytes);
    }

    private byte[] mergePdfBytes(List<byte[]> pdfBytes) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDFMergerUtility pdfMerger = new PDFMergerUtility();
            pdfMerger.setDestinationStream(outputStream);
            for (byte[] pdf : pdfBytes) {
                pdfMerger.addSource(new RandomAccessReadBuffer(pdf));
            }
            pdfMerger.mergeDocuments(IOUtils.createMemoryOnlyStreamCache());
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la fusion des PDFs", e);
        }
    }
}
