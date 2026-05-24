package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.Tuple4;
import com.kobe.warehouse.service.report.Constant;
import com.kobe.warehouse.service.report.pdf.AbstractStatistiqueReportService;
import com.kobe.warehouse.service.utils.NumberUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class FacturationPdfExportServiceImpl extends AbstractStatistiqueReportService implements FacturationPdfExportService {

    private final SpringTemplateEngine templateEngine;
    private final Map<String, Object> variablesMap = new HashMap<>();
    private static final String TEMPLATE_FILE = Constant.FACTURATION_TEMPLATE_FILE;

    public FacturationPdfExportServiceImpl(
        FileStorageProperties fileStorageProperties,
        SpringTemplateEngine templateEngine,
        StorageService storageService
    ) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
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
        return "facture";
    }

    private Tuple4 buildSummary(FactureTiersPayant factureTiersPayant) {
        long montantVente = 0;
        long montantAttendu = 0;
        long montantRemiseVente = 0;
        int montantRemiseForfaitaire = 0;
        for (ThirdPartySaleLine partySaleLine : factureTiersPayant.getFacturesDetails()) {
            ThirdPartySales thirdPartySales = partySaleLine.getSale();
            montantVente += thirdPartySales.getSalesAmount();
            montantAttendu += partySaleLine.getMontant();
            montantRemiseVente += Objects.requireNonNullElse(thirdPartySales.getDiscountAmount(), 0);
            montantRemiseForfaitaire += factureTiersPayant.getRemiseForfetaire();
        }
        return new Tuple4(montantVente, montantAttendu, montantRemiseVente, montantRemiseForfaitaire);
    }

    private byte[] generatePdf(FactureTiersPayant factureTiersPayant) {
        Tuple4 total = buildSummary(factureTiersPayant);
        getParameters().put(Constant.ENTITY, factureTiersPayant);
        getParameters().put(Constant.FACTURE_TOTAL, total);
        getParameters().put(Constant.FACTURE_TOTAL_LETTERS, NumberUtil.getNumberToWords(total.e2()).toUpperCase());
        return super.exportReportToPdf();
    }

    @Override
    public byte[] exportToPdf(FactureTiersPayant factureTiersPayant) {
        super.getCommonParameters();
        return generatePdf(factureTiersPayant);
    }

    @Override
    public byte[] exportToPdf(List<FactureTiersPayant> factureTiersPayants) {
        if (CollectionUtils.isEmpty(factureTiersPayants)) {
            throw new IllegalArgumentException("La liste des factures ne peut pas être vide");
        }
        super.getCommonParameters();
        List<byte[]> pdfBytes = factureTiersPayants.stream().map(this::generatePdf).toList();
        return mergePdfBytes(pdfBytes);
    }

    @Override
    public List<byte[]> exportAll(List<FactureTiersPayant> factureTiersPayants) {
        super.getCommonParameters();
        return factureTiersPayants.stream().map(this::generatePdf).toList();
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
