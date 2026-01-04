package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.EtiquetteDTO;
import com.kobe.warehouse.service.pdf.EtiquetteBarcodeReplacedElement;
import com.kobe.warehouse.service.report.CommonReportService;
import com.kobe.warehouse.service.report.Constant;
import com.kobe.warehouse.service.utils.NumberUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
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

    private byte[] printEtiquettes(List<EtiquetteDTO> items, int startAt) {
        Magasin magasin = storageService.getUser().getMagasin();
        this.items = items;
        templateFile = Constant.ETIQUETES_TEMPLATE_FILE;
        getParameters().put(Constant.MAGASIN, magasin);
        getParameters().put(Constant.ITEMS, ListUtils.partition(items, 5));
        getParameters().put(Constant.ETIQUETES_BEGIN, startAt);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
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
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des étiquettes", e);
        }
    }

    public byte[] export(List<OrderLine> orderLines, int startAt) {
        var magasin = storageService.getConnectedUserMagasin().getName().toUpperCase();
        return printEtiquettes(buildEtiquettes(orderLines, magasin, startAt), startAt);
    }

    private List<EtiquetteDTO> buildEtiquettes(List<OrderLine> orderLines, String rasionSociale, int startAt) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        List<EtiquetteDTO> etiquettes = new ArrayList<>();

        var finalItems = orderLines.stream().filter(e -> StringUtils.isNotEmpty(e.getFournisseurProduit().getCodeCip())).toList();
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
        List<OrderLine> finalItems,
        List<EtiquetteDTO> etiquettes
    ) {
        if (etiquettes.isEmpty()) {
            return finalItems.stream().map(e -> buildEtiquetteDTO(e, date, rasionSociale)).toList();
        }
        etiquettes.addAll(finalItems.stream().map(e -> buildEtiquetteDTO(e, date, rasionSociale)).toList());

        return etiquettes;
    }

    private EtiquetteDTO buildEtiquetteDTO(OrderLine item, String date, String rasionSociale) {
        FournisseurProduit fournisseurProduit = item.getFournisseurProduit();

        return new EtiquetteDTO()
            .setCode(fournisseurProduit.getCodeCip())
            .setPrix(String.format("%s CFA", NumberUtil.formatToString(fournisseurProduit.getPrixUni())))
            .setPrint(true)
            .setDate(date)
            .setMagasin(rasionSociale)
            .setLibelle(fournisseurProduit.getProduit().getLibelle());
    }

    }
