package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.enumeration.OngletRapportPilotage;
import com.kobe.warehouse.security.navaccess.NavAccessDeniedException;
import com.kobe.warehouse.security.navaccess.NavAccessService;
import com.kobe.warehouse.security.navaccess.NavAction;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.RequeteRapportPilotageDTO;
import com.kobe.warehouse.service.license.DemoWatermark;
import com.kobe.warehouse.service.pilotage.RapportAchatsStockService;
import com.kobe.warehouse.service.pilotage.RapportAnalyserService;
import com.kobe.warehouse.service.pilotage.RapportClientsEquipeService;
import com.kobe.warehouse.service.pilotage.RapportComparerAnneesService;
import com.kobe.warehouse.service.pilotage.RapportPilotageService;
import com.kobe.warehouse.service.pilotage.RapportRentabiliteService;
import com.kobe.warehouse.service.pilotage.RapportTableauDeBordService;
import com.kobe.warehouse.service.pilotage.RapportTresorerieService;
import com.kobe.warehouse.service.report.Constant;
import com.kobe.warehouse.service.report.pdf.AbstractStatistiqueReportService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

/**
 * Les données passent par un {@link Context} propre à chaque appel, jamais par l'état du service : deux rapports peuvent
 * s'imprimer en même temps.
 */
@Service
@Transactional(readOnly = true)
public class RapportPilotageServiceImpl extends AbstractStatistiqueReportService implements RapportPilotageService {

    private static final String GABARIT = "reports/pilotage/main";

    private final SpringTemplateEngine templateEngine;
    private final StorageService storageService;
    private final NavAccessService navAccessService;
    private final RapportTableauDeBordService rapportTableauDeBordService;
    private final RapportComparerAnneesService rapportComparerAnneesService;
    private final RapportRentabiliteService rapportRentabiliteService;
    private final RapportAchatsStockService rapportAchatsStockService;
    private final RapportTresorerieService rapportTresorerieService;
    private final RapportClientsEquipeService rapportClientsEquipeService;
    private final RapportAnalyserService rapportAnalyserService;

    public RapportPilotageServiceImpl(
        SpringTemplateEngine templateEngine,
        StorageService storageService,
        NavAccessService navAccessService,
        RapportTableauDeBordService rapportTableauDeBordService,
        RapportComparerAnneesService rapportComparerAnneesService,
        RapportRentabiliteService rapportRentabiliteService,
        RapportAchatsStockService rapportAchatsStockService,
        RapportTresorerieService rapportTresorerieService,
        RapportClientsEquipeService rapportClientsEquipeService,
        RapportAnalyserService rapportAnalyserService
    ) {
        super(storageService);
        this.templateEngine = templateEngine;
        this.storageService = storageService;
        this.navAccessService = navAccessService;
        this.rapportTableauDeBordService = rapportTableauDeBordService;
        this.rapportComparerAnneesService = rapportComparerAnneesService;
        this.rapportRentabiliteService = rapportRentabiliteService;
        this.rapportAchatsStockService = rapportAchatsStockService;
        this.rapportTresorerieService = rapportTresorerieService;
        this.rapportClientsEquipeService = rapportClientsEquipeService;
        this.rapportAnalyserService = rapportAnalyserService;
    }

    @Override
    public byte[] imprimerRapport(RequeteRapportPilotageDTO demande) {
        List<OngletRapportPilotage> onglets = demande.onglets().stream().distinct().toList();
        verifierDroits(onglets);
        RequetePilotageDTO requete = demande.requete();
        Context context = getContext();
        Magasin magasin = storageService.getUser().getMagasin();
        context.setVariable(Constant.MAGASIN, magasin);
        context.setVariable(Constant.FOOTER, "\"" + builderFooter(magasin) + "\"");
        context.setVariable(Constant.REPORT_TITLE, "Pilotage de l'officine du " + buildPeriode(requete.du(), requete.au()));
        context.setVariable("onglets", onglets);
        for (OngletRapportPilotage onglet : onglets) {
            context.setVariable(lireVariable(onglet), construireOnglet(onglet, demande));
        }
        return convertirEnPdf(getTemplateAsHtml(context));
    }

    private void verifierDroits(List<OngletRapportPilotage> onglets) {
        for (OngletRapportPilotage onglet : onglets) {
            if (!navAccessService.isAllowed(List.of(onglet.getCode()), NavAction.EXPORT)) {
                throw new NavAccessDeniedException("export de l'onglet « " + onglet.getLibelle() + " »");
            }
        }
    }

    private Object construireOnglet(OngletRapportPilotage onglet, RequeteRapportPilotageDTO demande) {
        return switch (onglet) {
            case TABLEAU_DE_BORD -> rapportTableauDeBordService.construireTableauDeBord(demande.requete());
            case ANALYSER -> rapportAnalyserService.construireAnalyse(demande.requete(), demande.analyse());
            case COMPARER_ANNEES -> rapportComparerAnneesService.construireComparaison(demande.lireReglageAnnees(), demande.libelleFiltreAnnees());
            case RENTABILITE_REMISES -> rapportRentabiliteService.construireRentabilite(demande.requete());
            case ACHATS_STOCK -> rapportAchatsStockService.construireAchatsStock(demande.requete());
            case TRESORERIE_TIERS_PAYANT -> rapportTresorerieService.construireTresorerie(demande.requete());
            case CLIENTS_EQUIPE -> rapportClientsEquipeService.construireClientsEquipe(demande.requete());
        };
    }

    /** Nom de la variable du gabarit : « tableauDeBord » pour TABLEAU_DE_BORD. */
    private static String lireVariable(OngletRapportPilotage onglet) {
        return switch (onglet) {
            case TABLEAU_DE_BORD -> "tableauDeBord";
            case ANALYSER -> "analyser";
            case COMPARER_ANNEES -> "comparerAnnees";
            case RENTABILITE_REMISES -> "rentabilite";
            case ACHATS_STOCK -> "achatsStock";
            case TRESORERIE_TIERS_PAYANT -> "tresorerie";
            case CLIENTS_EQUIPE -> "clientsEquipe";
        };
    }

    private static byte[] convertirEnPdf(String html) {
        try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(DemoWatermark.apply(html));
            renderer.layout();
            renderer.createPDF(sortie);
            return sortie.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected String getTemplateAsHtml(Context context) {
        return templateEngine.process(GABARIT, context);
    }

    @Override
    protected String getTemplateAsHtml() {
        throw new UnsupportedOperationException("Le rapport du pilotage se construit par imprimerRapport");
    }

    @Override
    protected Map<String, Object> getParameters() {
        return Map.of();
    }

    @Override
    protected String getGenerateFileName() {
        return "pilotage";
    }
}
