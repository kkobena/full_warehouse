package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.service.license.DemoWatermark;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.enumeration.FormatEtiquette;
import com.kobe.warehouse.domain.enumeration.ResolutionEtiquette;
import com.kobe.warehouse.domain.enumeration.TypeEtiquette;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.service.etiquette.zpl.ZplEtiquetteService;
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
    private final ZplEtiquetteService zplEtiquetteService;
    private final Map<String, Object> variablesMap = new HashMap<>();

    private String templateFile;
    private List<EtiquetteDTO> items;

    public EtiquetteExportReportServiceImpl(
        SpringTemplateEngine templateEngine,
        StorageService storageService,
        ZplEtiquetteService zplEtiquetteService
    ) {
        super( storageService);
        this.templateEngine = templateEngine;
        this.storageService = storageService;
        this.zplEtiquetteService = zplEtiquetteService;
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

    private byte[] printEtiquettes(List<EtiquetteDTO> items, int startAt, FormatEtiquette format) {
        Magasin magasin = storageService.getUser().getMagasin();
        this.items = items;
        // Un rouleau n'a pas de planche : son gabarit fixe la page a la taille de l'etiquette.
        templateFile = format.isRouleau() ? Constant.ETIQUETES_ROULEAU_TEMPLATE_FILE : Constant.ETIQUETES_TEMPLATE_FILE;
        getParameters().put(Constant.MAGASIN, magasin);
        getParameters().put(Constant.ITEMS, ListUtils.partition(items, format.getColonnes()));
        getParameters().put(Constant.ETIQUETES_BEGIN, startAt);
        getParameters().put(Constant.ETIQUETTES_FORMAT, format);
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
            renderer.setDocumentFromString(DemoWatermark.apply(this.getTemplateAsHtml()));
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des étiquettes", e);
        }
    }

    public byte[] export(List<OrderLine> orderLines, int startAt) {
        return export(orderLines, startAt, FormatEtiquette.parDefaut());
    }

    public byte[] export(List<OrderLine> orderLines, int startAt, FormatEtiquette format) {
        var magasin = storageService.getConnectedUserMagasin().getName().toUpperCase();
        int depart = format.normaliserCaseDeDepart(startAt);
        return printEtiquettes(buildEtiquettes(orderLines, magasin, depart), depart, format);
    }

    private List<EtiquetteDTO> buildEtiquettes(List<OrderLine> orderLines, String rasionSociale, int startAt) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        List<EtiquetteDTO> etiquettes = new ArrayList<>();

        var finalItems = orderLines.stream().filter(e -> StringUtils.isNotEmpty(e.getFournisseurProduit().getCodeCip())).toList();
        // startAt est la case de la PREMIERE etiquette : on saute les cases qui la precedent, donc
        // startAt - 1. La borne <= en sautait une de trop et decalait tout le bon d'une case.
        for (int i = 1; i < startAt; i++) {
            etiquettes.add(new EtiquetteDTO().setPrint(false));
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

    public byte[] exportForProduit(Produit produit, int qty, int startAt) {
        return exportForProduit(produit, qty, startAt, FormatEtiquette.parDefaut());
    }

    public byte[] exportForProduit(Produit produit, int qty, int startAt, FormatEtiquette format) {
        int depart = format.normaliserCaseDeDepart(startAt);
        List<EtiquetteDTO> etiquettes = new ArrayList<>();
        for (int i = 1; i < depart; i++) {
            etiquettes.add(new EtiquetteDTO().setPrint(false));
        }
        etiquettes.addAll(construireEtiquettesDuProduit(produit, qty));
        return printEtiquettes(etiquettes, depart, format);
    }

    /** Flux ZPL des étiquettes d'un bon : pour une imprimante à rouleau sans pilote exploitable. */
    public String genererZplPourBon(List<OrderLine> orderLines, FormatEtiquette format, ResolutionEtiquette resolution) {
        String magasin = storageService.getConnectedUserMagasin().getName().toUpperCase();
        // Pas de case de départ : un rouleau n'a pas de planche entamée à reprendre.
        return zplEtiquetteService.rendreEtiquettes(buildEtiquettes(orderLines, magasin, 1), TypeEtiquette.STANDARD, format, resolution);
    }

    /** Idem pour l'étiquette d'un article du catalogue, répétée {@code qty} fois. */
    public String genererZplPourProduit(Produit produit, int qty, FormatEtiquette format, ResolutionEtiquette resolution) {
        return zplEtiquetteService.rendreEtiquettes(construireEtiquettesDuProduit(produit, qty), TypeEtiquette.STANDARD, format, resolution);
    }

    private List<EtiquetteDTO> construireEtiquettesDuProduit(Produit produit, int qty) {
        String magasin = storageService.getConnectedUserMagasin().getName().toUpperCase();
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        FournisseurProduit fournisseurProduit = produit.getFournisseurProduitPrincipal();
        EtiquetteDTO dto = new EtiquetteDTO()
            .setCode(fournisseurProduit.getCodeCip())
            .setPrix(String.format("%s CFA", NumberUtil.formatToString(fournisseurProduit.getPrixUni())))
            .setPrint(true)
            .setDate(date)
            .setLibelle(produit.getLibelle())
            .setMagasin(magasin);
        List<EtiquetteDTO> etiquettes = new ArrayList<>(Math.max(qty, 0));
        for (int i = 0; i < qty; i++) {
            etiquettes.add(dto);
        }
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
