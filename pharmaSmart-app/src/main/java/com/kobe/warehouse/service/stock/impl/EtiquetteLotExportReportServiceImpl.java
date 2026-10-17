package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.FormatEtiquette;
import com.kobe.warehouse.domain.enumeration.ResolutionEtiquette;
import com.kobe.warehouse.domain.enumeration.TypeEtiquette;
import com.kobe.warehouse.repository.LotRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.EtiquetteDTO;
import com.kobe.warehouse.service.etiquette.zpl.ZplEtiquetteService;
import com.kobe.warehouse.service.license.DemoWatermark;
import com.kobe.warehouse.service.pdf.EtiquetteBarcodeReplacedElement;
import com.kobe.warehouse.service.pdf.Gs1DataMatrixEncoder;
import com.kobe.warehouse.service.report.CommonReportService;
import com.kobe.warehouse.service.report.Constant;
import com.kobe.warehouse.service.stock.EtiquetteLotExportService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.pdf.ITextReplacedElementFactory;

@Service
public class EtiquetteLotExportReportServiceImpl extends CommonReportService implements EtiquetteLotExportService {

    private static final DateTimeFormatter DATE_DU_JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    // Même format que les écrans et les autres PDF : l'étiquette est relue à côté du tableau des
    // lots de la fiche produit. Sur une boîte marquée « 05/2027 », le jour affiché est celui que
    // l'AI 17 à DD=00 fait déduire — la fin du mois.
    private static final DateTimeFormatter PEREMPTION_LISIBLE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final SpringTemplateEngine templateEngine;
    private final StorageService storageService;
    private final LotRepository lotRepository;
    private final ZplEtiquetteService zplEtiquetteService;
    private final Map<String, Object> variablesMap = new HashMap<>();

    private List<EtiquetteDTO> items = List.of();
    private String templateFile = Constant.ETIQUETES_LOT_TEMPLATE_FILE;

    public EtiquetteLotExportReportServiceImpl(
        SpringTemplateEngine templateEngine,
        StorageService storageService,
        LotRepository lotRepository,
        ZplEtiquetteService zplEtiquetteService
    ) {
        super(storageService);
        this.templateEngine = templateEngine;
        this.storageService = storageService;
        this.lotRepository = lotRepository;
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
        return "etiquettes-lot";
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exporterPourBon(List<OrderLine> orderLines, int startAt) {
        return exporterPourBon(orderLines, startAt, FormatEtiquette.parDefaut());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exporterPourBon(List<OrderLine> orderLines, int startAt, FormatEtiquette format) {
        int depart = format.normaliserCaseDeDepart(startAt);
        List<EtiquetteDTO> etiquettes = reserverLesCasesConsommees(depart);
        etiquettes.addAll(construireEtiquettesDuBon(orderLines));
        return imprimer(etiquettes, depart, format);
    }

    @Override
    @Transactional(readOnly = true)
    public String genererZplPourBon(List<OrderLine> orderLines, FormatEtiquette format, ResolutionEtiquette resolution) {
        return zplEtiquetteService.rendreEtiquettes(construireEtiquettesDuBon(orderLines), TypeEtiquette.LOT, format, resolution);
    }

    @Override
    @Transactional(readOnly = true)
    public String genererZplPourLot(Integer lotId, int qty, FormatEtiquette format, ResolutionEtiquette resolution) {
        return zplEtiquetteService.rendreEtiquettes(construireEtiquettesDuLot(lotId, qty), TypeEtiquette.LOT, format, resolution);
    }

    private List<EtiquetteDTO> construireEtiquettesDuBon(List<OrderLine> orderLines) {
        String raisonSociale = storageService.getConnectedUserMagasin().getName().toUpperCase();
        String date = LocalDate.now().format(DATE_DU_JOUR);
        List<EtiquetteDTO> etiquettes = new ArrayList<>();
        for (OrderLine ligne : orderLines) {
            for (Lot lot : ligne.getLots()) {
                etiquettes.add(construireEtiquette(ligne.getFournisseurProduit(), lot, date, raisonSociale));
            }
        }
        return etiquettes;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exporterPourLot(Integer lotId, int qty, int startAt) {
        return exporterPourLot(lotId, qty, startAt, FormatEtiquette.parDefaut());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exporterPourLot(Integer lotId, int qty, int startAt, FormatEtiquette format) {
        int depart = format.normaliserCaseDeDepart(startAt);
        List<EtiquetteDTO> etiquettes = reserverLesCasesConsommees(depart);
        etiquettes.addAll(construireEtiquettesDuLot(lotId, qty));
        return imprimer(etiquettes, depart, format);
    }

    private List<EtiquetteDTO> construireEtiquettesDuLot(Integer lotId, int qty) {
        Lot lot = lotRepository.findById(lotId).orElseThrow(() -> new IllegalArgumentException("Lot introuvable : " + lotId));
        Produit produit = lot.getProduit();
        String raisonSociale = storageService.getConnectedUserMagasin().getName().toUpperCase();
        EtiquetteDTO etiquette = construireEtiquette(
            produit.getFournisseurProduitPrincipal(),
            lot,
            LocalDate.now().format(DATE_DU_JOUR),
            raisonSociale
        );
        List<EtiquetteDTO> etiquettes = new ArrayList<>(Math.max(qty, 0));
        for (int i = 0; i < qty; i++) {
            etiquettes.add(etiquette);
        }
        return etiquettes;
    }

    /** Cases déjà consommées sur la planche : des étiquettes blanches les sautent. */
    private List<EtiquetteDTO> reserverLesCasesConsommees(int startAt) {
        List<EtiquetteDTO> etiquettes = new ArrayList<>();
        for (int i = 1; i < startAt; i++) {
            etiquettes.add(new EtiquetteDTO().setPrint(false));
        }
        return etiquettes;
    }

    private EtiquetteDTO construireEtiquette(FournisseurProduit fournisseurProduit, Lot lot, String date, String raisonSociale) {
        String code = fournisseurProduit.getCodeCip();
        EtiquetteDTO etiquette = new EtiquetteDTO()
            .setCode(code)
            .setLibelle(fournisseurProduit.getProduit().getLibelle())
            .setPrix(String.format("%s CFA", NumberUtil.formatToString(fournisseurProduit.getPrixUni())))
            .setPrint(true)
            .setDate(date)
            .setMagasin(raisonSociale)
            .setNumLot(lot.getNumLot());
        if (lot.getExpiryDate() != null) {
            etiquette.setExpiryDate(lot.getExpiryDate().format(PEREMPTION_LISIBLE));
        }
        etiquette.setCodeDataMatrix(Gs1DataMatrixEncoder.construireEntree(code, lot.getNumLot(), lot.getExpiryDate()));
        return etiquette;
    }

    private byte[] imprimer(List<EtiquetteDTO> etiquettes, int startAt, FormatEtiquette format) {
        Magasin magasin = storageService.getUser().getMagasin();
        this.items = etiquettes;
        // Un rouleau n'a pas de planche : son gabarit fixe la page a la taille de l'etiquette.
        this.templateFile = format.isRouleau() ? Constant.ETIQUETES_ROULEAU_LOT_TEMPLATE_FILE : Constant.ETIQUETES_LOT_TEMPLATE_FILE;
        getParameters().put(Constant.MAGASIN, magasin);
        getParameters().put(Constant.ITEMS, ListUtils.partition(etiquettes, format.getColonnes()));
        getParameters().put(Constant.ETIQUETES_BEGIN, startAt);
        getParameters().put(Constant.ETIQUETTES_FORMAT, format);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            SharedContext sharedContext = renderer.getSharedContext();
            EtiquetteBarcodeReplacedElement ref = new EtiquetteBarcodeReplacedElement(
                new ITextReplacedElementFactory(renderer.getOutputDevice())
            );
            ref.setBarcodesData(listerCodesBarres(etiquettes));
            sharedContext.setReplacedElementFactory(ref);
            sharedContext.getTextRenderer().setSmoothingThreshold(0);
            sharedContext.setPrint(true);
            renderer.setDocumentFromString(DemoWatermark.apply(this.getTemplateAsHtml()));
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors de la génération du PDF des étiquettes de lot", e);
        }
    }

    /** Un seul passage : la trame DataMatrix, et le Code 39 du code quand elle manque. */
    private List<String> listerCodesBarres(List<EtiquetteDTO> etiquettes) {
        List<String> codes = new ArrayList<>(etiquettes.size());
        for (EtiquetteDTO etiquette : etiquettes) {
            if (etiquette.getCodeDataMatrix() != null) {
                codes.add(etiquette.getCodeDataMatrix());
            } else if (StringUtils.hasText(etiquette.getCode())) {
                codes.add(etiquette.getCode());
            }
        }
        return codes;
    }
}
