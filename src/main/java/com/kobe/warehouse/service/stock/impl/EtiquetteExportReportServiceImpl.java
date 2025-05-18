package com.kobe.warehouse.service.stock.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.DeliveryReceiptItem;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.EtiquetteDTO;
import com.kobe.warehouse.service.pdf.EtiquetteBarcodeReplacedElement;
import com.kobe.warehouse.service.report.CommonReportService;
import com.kobe.warehouse.service.report.Constant;
import com.kobe.warehouse.service.utils.NumberUtil;
import com.lowagie.text.DocumentException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.pdf.ITextReplacedElementFactory;

@Service
public class EtiquetteExportReportServiceImpl extends CommonReportService {

    private final SpringTemplateEngine templateEngine;
    private final StorageService storageService;
    private final Map<String, Object> variablesMap = new HashMap<>();

    private String templateFile;
    private List<EtiquetteDTO> items;

    public EtiquetteExportReportServiceImpl(
        FileStorageProperties fileStorageProperties,
        SpringTemplateEngine templateEngine,
        StorageService storageService
    ) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
        this.storageService = storageService;
    }

    @Override
    protected List<EtiquetteDTO> getItems() {
        return this.items;
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
        return "etiquettes";
    }

    private String printEtiquettes(List<EtiquetteDTO> items, int startAt) {
        Magasin magasin = storageService.getUser().getMagasin();
        this.items = items;
        templateFile = Constant.ETIQUETES_TEMPLATE_FILE;
        getParameters().put(Constant.MAGASIN, magasin);
        getParameters().put(Constant.ITEMS, ListUtils.partition(items, 5));
        getParameters().put(Constant.ETIQUETES_BEGIN, startAt);
        String filePath = getDestFilePath();
        try (OutputStream outputStream = new FileOutputStream(filePath)) {
            ITextRenderer renderer = new ITextRenderer();
            SharedContext sharedContext = renderer.getSharedContext();
            EtiquetteBarcodeReplacedElement ref = new EtiquetteBarcodeReplacedElement(
                new ITextReplacedElementFactory(renderer.getOutputDevice())
            );
            ref.setBarcodesData(items.stream().map(EtiquetteDTO::getCode).toList());
            sharedContext.setReplacedElementFactory(ref);
            sharedContext.getTextRenderer().setSmoothingThreshold(0);
            sharedContext.setPrint(true);
            renderer.setDocumentFromString(this.getTemplateAsHtml());

            renderer.layout();
            renderer.createPDF(outputStream);
        } catch (IOException | DocumentException e) {
            log.error("printOneReceiptPage ===>>", e);
        }
        return filePath;
    }

    public Resource print(List<DeliveryReceiptItem> receiptItems, int startAt) throws MalformedURLException {
        var magasin = storageService.getConnectedUserMagasin().getName().toUpperCase();
        return this.getResource(printEtiquettes(buildEtiquettes(receiptItems, magasin, startAt), startAt));
    }

    private List<EtiquetteDTO> buildEtiquettes(List<DeliveryReceiptItem> receiptItems, String rasionSociale, int startAt) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        List<EtiquetteDTO> etiquettes = new ArrayList<>();

        var finalItems = receiptItems.stream().filter(e -> StringUtils.isNotEmpty(e.getFournisseurProduit().getCodeCip())).toList();
        //   int index = 1;
        if (startAt > 1) {
            for (int i = 1; i <= startAt; i++) {
                etiquettes.add(new EtiquetteDTO().setPrint(false));
                //  index++;
            }
            // return getEtiquetteDTOS(rasionSociale, date, etiquettes, finalItems, index, Comparator.comparing(EtiquetteDTO::getOrder));
        }
        return getEtiquetteDTOS(rasionSociale, date, finalItems, etiquettes);
    }

    private List<EtiquetteDTO> getEtiquetteDTOS(
        String rasionSociale,
        String date,
        List<DeliveryReceiptItem> finalItems,
        List<EtiquetteDTO> etiquettes
    ) {
        if (etiquettes.isEmpty()) {
            return finalItems.stream().map(e -> buildEtiquetteDTO(e, date, rasionSociale)).toList();
        }
        etiquettes.addAll(finalItems.stream().map(e -> buildEtiquetteDTO(e, date, rasionSociale)).toList());
        return etiquettes;
    }

    private EtiquetteDTO buildEtiquetteDTO(DeliveryReceiptItem item, String date, String rasionSociale) {
        FournisseurProduit fournisseurProduit = item.getFournisseurProduit();

        return new EtiquetteDTO()
            .setCode(fournisseurProduit.getCodeCip())
            .setPrix(String.format("%s CFA", NumberUtil.formatToString(item.getRegularUnitPrice())))
            .setPrint(true)
            .setDate(date)
            .setMagasin(rasionSociale)
            .setLibelle(fournisseurProduit.getProduit().getLibelle());
    }


    private String generateBarcodeImage(String code) throws WriterException, IOException {
        BitMatrix matrix = new MultiFormatWriter().encode(code, BarcodeFormat.CODE_128, 200, 50);
        File tempFile = File.createTempFile("barcode-" + code, ".png");
        MatrixToImageWriter.writeToPath(matrix, "PNG", tempFile.toPath());
        return tempFile.getAbsolutePath();
    }

}
