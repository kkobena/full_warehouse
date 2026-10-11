package com.kobe.warehouse.service.pilotage.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.enumeration.AffichageAnalyse;
import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.GraviteAlerte;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.ModeAnnees;
import com.kobe.warehouse.domain.enumeration.OngletRapportPilotage;
import com.kobe.warehouse.domain.enumeration.TriAnalyse;
import com.kobe.warehouse.domain.enumeration.UniteIndicateur;
import com.kobe.warehouse.security.navaccess.NavAccessDeniedException;
import com.kobe.warehouse.security.navaccess.NavAccessService;
import com.kobe.warehouse.security.navaccess.NavAction;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.pilotage.AlertePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.AnneeCompareeDTO;
import com.kobe.warehouse.service.dto.pilotage.AxeAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.CelluleAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonAnneesDTO;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.CroiseAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.DemarqueRentabiliteDTO;
import com.kobe.warehouse.service.dto.pilotage.IndicateurPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneCroiseeDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneDemarqueDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneMargeDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneRemiseDTO;
import com.kobe.warehouse.service.dto.pilotage.MargeRentabiliteDTO;
import com.kobe.warehouse.service.dto.pilotage.MembreAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.MoisRecordDTO;
import com.kobe.warehouse.service.dto.pilotage.MontantDemarqueDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.dto.pilotage.PointSerieDTO;
import com.kobe.warehouse.service.dto.pilotage.ReglageAnalyseRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.RemisesRentabiliteDTO;
import com.kobe.warehouse.service.dto.pilotage.RequeteAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.RequeteAnneesDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.RequeteRapportPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.SerieIndicateurDTO;
import com.kobe.warehouse.service.dto.pilotage.SeriesPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.SyntheseAnneeDTO;
import com.kobe.warehouse.service.dto.pilotage.VenteMargeDTO;
import com.kobe.warehouse.service.dto.pilotage.VenteRemiseeDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.FormatGraphe;
import com.kobe.warehouse.service.dto.pilotage.rapport.LigneRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.LigneRapportDTO.CelluleRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.MontantRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.SerieGraphiqueDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.SerieGraphiqueDTO.StyleSerie;
import com.kobe.warehouse.service.dto.pilotage.rapport.TableauDeBordRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.TableauRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.TuileRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.VariationRapportDTO;
import com.kobe.warehouse.service.pilotage.AchatsPilotageService;
import com.kobe.warehouse.service.pilotage.AnalysePilotageService;
import com.kobe.warehouse.service.pilotage.ClientsEquipePilotageService;
import com.kobe.warehouse.service.pilotage.ComparaisonAnneesService;
import com.kobe.warehouse.service.pilotage.StockPilotageService;
import com.kobe.warehouse.service.pilotage.TresoreriePilotageService;
import com.kobe.warehouse.service.pilotage.DemarquePilotageService;
import com.kobe.warehouse.service.pilotage.MargePilotageService;
import com.kobe.warehouse.service.pilotage.RapportTableauDeBordService;
import com.kobe.warehouse.service.pilotage.RemisesPilotageService;
import com.kobe.warehouse.service.pilotage.SeriesPilotageService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;
import org.openpdf.text.pdf.PdfName;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class RapportPilotageServiceImplTest {

    private static final RequetePilotageDTO REQUETE = new RequetePilotageDTO(
        LocalDate.of(2026, 9, 1),
        LocalDate.of(2026, 9, 30),
        null,
        null,
        null,
        null,
        null,
        null,
        null
    );

    private final NavAccessService navAccessService = mock(NavAccessService.class);
    private final RapportTableauDeBordService rapportTableauDeBordService = mock(RapportTableauDeBordService.class);
    private final ComparaisonAnneesService comparaisonAnneesService = mock(ComparaisonAnneesService.class);
    private final SeriesPilotageService seriesPilotageService = mock(SeriesPilotageService.class);
    private final MargePilotageService margePilotageService = mock(MargePilotageService.class);
    private final RemisesPilotageService remisesPilotageService = mock(RemisesPilotageService.class);
    private final DemarquePilotageService demarquePilotageService = mock(DemarquePilotageService.class);
    private final AchatsPilotageService achatsPilotageService = mock(AchatsPilotageService.class);
    private final StockPilotageService stockPilotageService = mock(StockPilotageService.class);
    private final TresoreriePilotageService tresoreriePilotageService = mock(TresoreriePilotageService.class);
    private final ClientsEquipePilotageService clientsEquipePilotageService = mock(ClientsEquipePilotageService.class);
    private final AnalysePilotageService analysePilotageService = mock(AnalysePilotageService.class);

    @Test
    @Timeout(60)
    void imprimeLesAchatsEtLeStockSansLesTableauxVides() throws Exception {
        when(navAccessService.isAllowed(any(), eq(NavAction.EXPORT))).thenReturn(true);
        preparerSeries();
        when(achatsPilotageService.analyserAchats(any())).thenReturn(JeuRapportPilotage.lireAchats());
        when(achatsPilotageService.analyserAchatsVentes(any())).thenReturn(JeuRapportPilotage.lireAchatsVentes());
        when(stockPilotageService.analyserStock(any())).thenReturn(JeuRapportPilotage.lireStock());
        when(stockPilotageService.analyserRuptures(any())).thenReturn(JeuRapportPilotage.lireRuptures());

        byte[] pdf = lireService().imprimerRapport(lireDemande(OngletRapportPilotage.ACHATS_STOCK));

        try (PdfReader reader = new PdfReader(pdf)) {
            assertThat(lireTexte(reader))
                .contains("Achats & stock")
                .contains("Laborex")
                .contains("Ventes / achats sur 12 mois")
                .contains("Fin septembre 2026")
                .contains("Produits dormants (sans vente depuis 90 jours)")
                .contains("Jamais")
                .contains("Ruptures par fournisseur")
                .contains("Ventes manquées par produit")
                .contains("décembre 2026")
                .doesNotContain("Ruptures par produit");
        }
    }

    @Test
    @Timeout(60)
    void imprimeLaTresorerieAvecLeDelaiRetenuDesOrganismes() throws Exception {
        when(navAccessService.isAllowed(any(), eq(NavAction.EXPORT))).thenReturn(true);
        preparerSeries();
        when(tresoreriePilotageService.analyserEncaissements(any())).thenReturn(JeuRapportPilotage.lireEncaissements());
        when(tresoreriePilotageService.analyserTiersPayant(any(), any())).thenReturn(JeuRapportPilotage.lireTiersPayant());
        when(tresoreriePilotageService.analyserDifferes(any(), any())).thenReturn(JeuRapportPilotage.lireDifferes());

        byte[] pdf = lireService()
            .imprimerRapport(lireDemande(OngletRapportPilotage.TRESORERIE_TIERS_PAYANT));

        try (PdfReader reader = new PdfReader(pdf)) {
            assertThat(lireTexte(reader))
                .contains("Trésorerie & tiers payant")
                .contains("dont Espèces")
                .contains("Par caissier")
                .contains("MUGEF-CI")
                .contains("60 j")
                .contains("au moins 3 factures réglées")
                .contains("Kouassi Yao");
        }
    }

    @Test
    @Timeout(60)
    void imprimeLAnalyseEnBarresAvecLeCheminEtLeDetail() throws Exception {
        when(navAccessService.isAllowed(any(), eq(NavAction.EXPORT))).thenReturn(true);
        when(analysePilotageService.analyser(any(), any())).thenReturn(JeuRapportPilotage.lireAnalyseVentilee());
        ReglageAnalyseRapportDTO reglage = new ReglageAnalyseRapportDTO(
            List.of(IndicateurPilotage.CA_TTC, IndicateurPilotage.TAUX_MARGE),
            new RequeteAnalyseDTO(AxeAnalyse.PRODUIT, null, 20, TriAnalyse.ECART_BAISSE, List.of("FAMILLE:12")),
            AffichageAnalyse.BARRES,
            List.of("Antalgiques")
        );

        byte[] pdf = lireService().imprimerRapport(new RequeteRapportPilotageDTO(REQUETE, List.of(OngletRapportPilotage.ANALYSER), null, null, reglage));

        ArgumentCaptor<RequetePilotageDTO> requete = ArgumentCaptor.forClass(RequetePilotageDTO.class);
        ArgumentCaptor<RequeteAnalyseDTO> analyse = ArgumentCaptor.forClass(RequeteAnalyseDTO.class);
        verify(analysePilotageService).analyser(requete.capture(), analyse.capture());
        assertThat(requete.getValue().indicateurs()).containsExactly(IndicateurPilotage.CA_TTC, IndicateurPilotage.TAUX_MARGE);
        assertThat(analyse.getValue().filtre()).containsExactly("FAMILLE:12");
        try (PdfReader reader = new PdfReader(pdf)) {
            assertThat(lireTexte(reader))
                .contains("Analyser")
                .contains("plus fortes baisses d'abord")
                .contains("Tout › Antalgiques")
                .contains("Chiffre d'affaires TTC par produit")
                .contains("Détail par produit")
                .contains("Doliprane 1000")
                .contains("Autres")
                .contains("Total (3)")
                .contains("Contribution à l'écart");
            assertThat(reader.getPageN(1).getAsDict(PdfName.RESOURCES).getAsDict(PdfName.XOBJECT)).isNotNull();
        }
    }

    @Test
    @Timeout(60)
    void suitLesElementsDansLeTempsEnCourbeSurLaPeriode() throws Exception {
        when(navAccessService.isAllowed(any(), eq(NavAction.EXPORT))).thenReturn(true);
        when(analysePilotageService.analyser(any(), any())).thenReturn(JeuRapportPilotage.lireAnalyseVentilee());
        ReglageAnalyseRapportDTO reglage = new ReglageAnalyseRapportDTO(
            List.of(IndicateurPilotage.CA_TTC),
            new RequeteAnalyseDTO(AxeAnalyse.FAMILLE, null, 20, null, null),
            AffichageAnalyse.COURBE,
            null
        );

        lireService().imprimerRapport(new RequeteRapportPilotageDTO(REQUETE, List.of(OngletRapportPilotage.ANALYSER), null, null, reglage));

        // La ventilation de la période, puis la courbe : second axe PERIODE, huit courbes au plus, comme l'écran.
        ArgumentCaptor<RequeteAnalyseDTO> analyses = ArgumentCaptor.forClass(RequeteAnalyseDTO.class);
        verify(analysePilotageService, times(2)).analyser(any(), analyses.capture());
        RequeteAnalyseDTO courbe = analyses.getAllValues().get(1);
        assertThat(courbe.axe2()).isEqualTo(AxeAnalyse.PERIODE);
        assertThat(courbe.top()).isEqualTo(8);
    }

    @Test
    @Timeout(60)
    void imprimeLaFrequentationEnCarteDeChaleurEtLEquipe() throws Exception {
        when(navAccessService.isAllowed(any(), eq(NavAction.EXPORT))).thenReturn(true);
        preparerSeries();
        when(analysePilotageService.analyser(any(), any())).thenReturn(JeuRapportPilotage.lireAnalyseCroisee());
        when(clientsEquipePilotageService.analyserClients(any())).thenReturn(JeuRapportPilotage.lireClients());
        when(clientsEquipePilotageService.analyserEquipe(any())).thenReturn(JeuRapportPilotage.lireEquipe());

        byte[] pdf = lireService().imprimerRapport(lireDemande(OngletRapportPilotage.CLIENTS_EQUIPE));

        try (PdfReader reader = new PdfReader(pdf)) {
            assertThat(lireTexte(reader))
                .contains("Clients & équipe")
                .contains("Créneaux les plus chargés : mardi 10 h (42 ventes), lundi 10 h (30 ventes), lundi 9 h (12 ventes).")
                .contains("Revenus")
                .contains("Kouassi Yao")
                .contains("Équipe entière")
                .contains("Vendeur / Heure");
        }
    }

    @Test
    @Timeout(60)
    void imprimeLaRentabiliteAvecSesTroisSousSections() throws Exception {
        when(navAccessService.isAllowed(any(), eq(NavAction.EXPORT))).thenReturn(true);
        preparerRentabilite();

        byte[] pdf = lireService().imprimerRapport(lireDemande(OngletRapportPilotage.RENTABILITE_REMISES));

        try (PdfReader reader = new PdfReader(pdf)) {
            assertThat(lireTexte(reader))
                .contains("Rentabilité & remises")
                .contains("Le taux de marge progresse de 0,6 pt : + 0,4 pt dû au mix")
                .contains("Faibles marges (sous 15 %)")
                .contains("Doliprane 1000")
                .contains("Marge par élément")
                .contains("V-0012")
                .contains("Plus de 2 fois le taux de l'équipe")
                .contains("plus de 2 × le taux moyen")
                .contains("Privilège du vendeur")
                .contains("Périmés")
                .contains("Amoxicilline 500");
        }
    }

    @Test
    @Timeout(60)
    void imprimeLeTableauDeBordAvecSaCourbe() throws Exception {
        when(navAccessService.isAllowed(List.of("pilotage.tableau-de-bord"), NavAction.EXPORT)).thenReturn(true);
        when(rapportTableauDeBordService.construireTableauDeBord(any())).thenReturn(lireTableauDeBord());

        byte[] pdf = lireService().imprimerRapport(lireDemande(OngletRapportPilotage.TABLEAU_DE_BORD));

        assertThat(pdf).startsWith("%PDF".getBytes());
        try (PdfReader reader = new PdfReader(pdf)) {
            String texte = new PdfTextExtractor(reader).getTextFromPage(1);
            assertThat(texte)
                .contains("Pharmacie du Test")
                .contains("Tableau de bord")
                .contains("Chiffre d'affaires TTC")
                .contains("19,2 M")
                .contains("+ 4,2 %")
                .contains("Rupture fournisseur")
                .contains("Détail par période");
            assertThat(reader.getPageN(1).getAsDict(PdfName.RESOURCES).getAsDict(PdfName.XOBJECT)).isNotNull();
        }
    }

    @Test
    @Timeout(60)
    void imprimeChaqueOngletDemandeDansLOrdreAvecLeReglageDeLEcran() throws Exception {
        when(navAccessService.isAllowed(any(), eq(NavAction.EXPORT))).thenReturn(true);
        when(rapportTableauDeBordService.construireTableauDeBord(any())).thenReturn(lireTableauDeBord());
        when(comparaisonAnneesService.comparerAnnees(any(), any())).thenReturn(lireComparaisonAnnees());
        RequeteAnneesDTO reglage = new RequeteAnneesDTO(IndicateurPilotage.CA_TTC, 2, ModeAnnees.MENSUEL, null, false, List.of("FAMILLE:12"));
        RequeteRapportPilotageDTO demande = new RequeteRapportPilotageDTO(
            REQUETE,
            List.of(OngletRapportPilotage.TABLEAU_DE_BORD, OngletRapportPilotage.COMPARER_ANNEES),
            reglage,
            "Antalgiques",
            null
        );

        byte[] pdf = lireService().imprimerRapport(demande);
        ArgumentCaptor<RequeteAnneesDTO> envoye = ArgumentCaptor.forClass(RequeteAnneesDTO.class);
        verify(comparaisonAnneesService).comparerAnnees(envoye.capture(), any());
        // « À date » vient de la barre de période, pas du réglage de l'onglet.
        assertThat(envoye.getValue().aDate()).isTrue();
        assertThat(envoye.getValue().filtre()).containsExactly("FAMILLE:12");
        try (PdfReader reader = new PdfReader(pdf)) {
            String texte = lireTexte(reader);
            assertThat(texte.indexOf("Tableau de bord")).isLessThan(texte.indexOf("Comparer les années"));
            assertThat(texte)
                .contains("filtré sur Antalgiques")
                .contains("Mois le plus élevé : décembre 2025")
                .contains("Croissance annuelle moyenne 2024-2025 : + 4,2 %")
                .contains("Saisonnalité moyenne")
                .contains("Mois / Année")
                .contains("2026 (en cours)")
                .contains("Synthèse annuelle")
                .contains("Famille / Année")
                .doesNotContain("Types de vente × années");
        }
    }

    @Test
    void refuseToutLeRapportSansLeDroitDExportDUnOnglet() {
        when(navAccessService.isAllowed(List.of("pilotage.tableau-de-bord"), NavAction.EXPORT)).thenReturn(true);
        when(navAccessService.isAllowed(List.of("pilotage.comparer-annees"), NavAction.EXPORT)).thenReturn(false);

        RapportPilotageServiceImpl service = lireService();
        RequeteRapportPilotageDTO demande = new RequeteRapportPilotageDTO(
            REQUETE,
            List.of(OngletRapportPilotage.TABLEAU_DE_BORD, OngletRapportPilotage.COMPARER_ANNEES),
            null,
            null,
            null
        );

        assertThatThrownBy(() -> service.imprimerRapport(demande)).isInstanceOf(NavAccessDeniedException.class).hasMessageContaining("Comparer les années");
        verifyNoInteractions(rapportTableauDeBordService, comparaisonAnneesService);
    }

    private static RequeteRapportPilotageDTO lireDemande(OngletRapportPilotage onglet) {
        return new RequeteRapportPilotageDTO(REQUETE, List.of(onglet), null, null, null);
    }

    private RapportPilotageServiceImpl lireService() {
        StorageService storageService = mock(StorageService.class);
        Magasin magasin = new Magasin();
        magasin.setName("Pharmacie du Test");
        AppUser utilisateur = new AppUser();
        utilisateur.setMagasin(magasin);
        when(storageService.getUser()).thenReturn(utilisateur);
        GraphiquePilotageServiceImpl graphique = new GraphiquePilotageServiceImpl();
        RapportComparerAnneesServiceImpl rapportComparerAnnees = new RapportComparerAnneesServiceImpl(comparaisonAnneesService, graphique);
        RapportRentabiliteServiceImpl rapportRentabilite = new RapportRentabiliteServiceImpl(
            seriesPilotageService,
            margePilotageService,
            remisesPilotageService,
            demarquePilotageService,
            new RapportCourbesServiceImpl(seriesPilotageService, graphique)
        );
        RapportCourbesServiceImpl courbes = new RapportCourbesServiceImpl(seriesPilotageService, graphique);
        return new RapportPilotageServiceImpl(
            lireMoteur(),
            storageService,
            navAccessService,
            rapportTableauDeBordService,
            rapportComparerAnnees,
            rapportRentabilite,
            new RapportAchatsStockServiceImpl(achatsPilotageService, stockPilotageService, seriesPilotageService, courbes, graphique),
            new RapportTresorerieServiceImpl(tresoreriePilotageService, courbes, graphique),
            new RapportClientsEquipeServiceImpl(clientsEquipePilotageService, analysePilotageService, courbes),
            new RapportAnalyserServiceImpl(analysePilotageService, graphique)
        );
    }

    /** Toute série demandée (tuiles, courbes) : le taux de marge sur deux mois. */
    private void preparerSeries() {
        List<PointSerieDTO> points = List.of(
            new PointSerieDTO(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), "Août 2026", 30.2, 29.8, 0.4, 1.3, null),
            new PointSerieDTO(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), "Sept. 2026", 31.0, 30.4, 0.6, 2.0, null)
        );
        SerieIndicateurDTO tauxMarge = new SerieIndicateurDTO(
            IndicateurPilotageDTO.fromIndicateur(IndicateurPilotage.TAUX_MARGE),
            31.0,
            30.4,
            0.6,
            2.0,
            points,
            null,
            null
        );
        when(seriesPilotageService.calculerSeries(any())).thenReturn(new SeriesPilotageDTO(JeuRapportPilotage.COMPARAISON, 26, 26, List.of(tauxMarge)));
    }

    /** Taux de marge en hausse de 0,6 pt (0,4 dû au mix), une vente à perte, un vendeur et une remise signalés. */
    private void preparerRentabilite() {
        preparerSeries();
        ComparaisonPeriodesDTO comparaison = JeuRapportPilotage.COMPARAISON;
        LigneMargeDTO famille = new LigneMargeDTO("1", "Antalgiques", 1_000_000, 700_000, 300_000, 30.0, 29.0, 1.0, 20_000L, 10_000L);
        LigneMargeDTO faible = new LigneMargeDTO("2", "Doliprane 1000", 50_000, 47_000, 3_000, 6.0, null, null, null, null);
        VenteMargeDTO aPerte = new VenteMargeDTO(1L, LocalDate.of(2026, 9, 12), "V-0012", "Awa", 1_000d, 1_200d, -200d);
        when(margePilotageService.analyserMarge(any(), eq(AxeAnalyse.FAMILLE))).thenReturn(
            new MargeRentabiliteDTO(comparaison, AxeAnalyseDTO.fromAxe(AxeAnalyse.FAMILLE), 31.0, 30.4, 0.4, 0.2, List.of(famille), 15, List.of(faible), List.of(aPerte))
        );
        CelluleAnalyseDTO cellule = new CelluleAnalyseDTO(100_000d, 80_000d, 20_000d, 25.0);
        CelluleAnalyseDTO taux = new CelluleAnalyseDTO(10.0, 8.0, 2.0, 25.0);
        // Taux moyen des ventes remisées : 100 000 / 1 000 000 = 10 % ; une remise à 50 % dépasse deux fois ce taux.
        CelluleAnalyseDTO caRemise = new CelluleAnalyseDTO(1_000_000d, 900_000d, 100_000d, 11.1);
        LigneRemiseDTO privilege = new LigneRemiseDTO("PRIVILEGE", "Privilège du vendeur", cellule, caRemise, cellule, taux, null, false);
        LigneRemiseDTO vendeur = new LigneRemiseDTO("7", "Awa", cellule, cellule, cellule, taux, null, true);
        VenteRemiseeDTO forte = new VenteRemiseeDTO(2L, LocalDate.of(2026, 9, 14), "V-0013", "Awa", 10_000, 5_000, 50.0, null);
        when(remisesPilotageService.analyserRemises(any())).thenReturn(
            new RemisesRentabiliteDTO(comparaison, cellule, taux, taux, cellule, taux, List.of(privilege), List.of(), List.of(vendeur), 2, List.of(forte))
        );
        when(demarquePilotageService.analyserDemarque(any())).thenReturn(
            new DemarqueRentabiliteDTO(
                comparaison,
                cellule,
                taux,
                List.of(new LigneDemarqueDTO("Périmés", 12, cellule)),
                List.of(new MontantDemarqueDTO("p", "Amoxicilline 500", 5L, 15_000L))
            )
        );
    }

    private static String lireTexte(PdfReader reader) throws Exception {
        PdfTextExtractor extracteur = new PdfTextExtractor(reader);
        StringBuilder texte = new StringBuilder();
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            texte.append(extracteur.getTextFromPage(page)).append('\n');
        }
        return texte.toString();
    }

    private static TableauDeBordRapportDTO lireTableauDeBord() {
        List<String> libelles = List.of("juil. 2026", "août 2026", "sept. 2026");
        String courbe = new GraphiquePilotageServiceImpl()
            .dessinerCourbes(
                libelles,
                List.of(
                    new SerieGraphiqueDTO("Réalisé", List.of(18_000_000d, 17_500_000d, 19_200_000d), StyleSerie.REALISE),
                    new SerieGraphiqueDTO("N-1", Arrays.asList(17_000_000d, null, 18_400_000d), StyleSerie.REFERENCE)
                ),
                UniteIndicateur.MONTANT,
                FormatGraphe.NORMAL
            );
        VariationRapportDTO hausse = new VariationRapportDTO("+ 4,2 %", "favorable");
        return new TableauDeBordRapportDTO(
            "du 01/09/2026 au 30/09/2026",
            List.of(new AlertePilotageDTO("RUPTURE", GraviteAlerte.HAUTE, "Rupture fournisseur", "3 produits", "achats-stock")),
            List.of(new TuileRapportDTO("Chiffre d'affaires TTC", "19,2 M", hausse, List.of("CA net de remises : 18,9 M"))),
            "Chiffre d'affaires TTC — 13 derniers mois",
            courbe,
            new MontantRapportDTO("Écart de CA", "+ 800 k", "favorable"),
            List.of(new MontantRapportDTO("Fréquentation (nombre de ventes)", "+ 500 k", "favorable")),
            List.of(),
            List.of(),
            new TableauRapportDTO(
                "Période",
                List.of("Chiffre d'affaires TTC"),
                List.of(new LigneRapportDTO("sept. 2026", List.of(new CelluleRapportDTO("19,2 M", hausse))))
            )
        );
    }

    /** 2025 close, 2026 en cours au 10 octobre ; une famille, pas de ventilation par type de vente. */
    private static ComparaisonAnneesDTO lireComparaisonAnnees() {
        CelluleAnalyseDTO cellule = new CelluleAnalyseDTO(1_500_000d, 1_400_000d, 100_000d, 7.1);
        CelluleAnalyseDTO vide = new CelluleAnalyseDTO(null, null, null, null);
        List<CelluleAnalyseDTO> moisEnCours = new ArrayList<>(Collections.nCopies(9, cellule));
        moisEnCours.add(new CelluleAnalyseDTO(1_900_000d, 1_500_000d, 400_000d, 26.7));
        moisEnCours.addAll(Collections.nCopies(2, vide));
        List<CelluleAnalyseDTO> mois2025 = new ArrayList<>(Collections.nCopies(11, cellule));
        mois2025.add(new CelluleAnalyseDTO(2_100_000d, 1_800_000d, 300_000d, 16.7));
        AnneeCompareeDTO annee2025 = new AnneeCompareeDTO(2025, true, mois2025, Collections.nCopies(4, cellule), cellule);
        AnneeCompareeDTO annee2026 = new AnneeCompareeDTO(2026, false, moisEnCours, Collections.nCopies(4, cellule), cellule);
        CroiseAnalyseDTO familles = new CroiseAnalyseDTO(
            List.of(new MembreAnalyseDTO("2025", "2025"), new MembreAnalyseDTO("2026", "2026 (en cours)")),
            List.of(new LigneCroiseeDTO("12", "Antalgiques", List.of(cellule, cellule)))
        );
        return new ComparaisonAnneesDTO(
            IndicateurPilotageDTO.fromIndicateur(IndicateurPilotage.CA_TTC),
            ModeAnnees.MENSUEL,
            false,
            LocalDate.of(2026, 10, 10),
            List.of(annee2025, annee2026),
            Collections.nCopies(12, 8.3),
            4.2,
            2024,
            2025,
            new MoisRecordDTO(2025, 12, 2_100_000),
            List.of(
                new SyntheseAnneeDTO(2025, true, 18_000_000d, 5_000_000d, 31.2, 12_000d, 1_500d, 200_000d, 3.5),
                new SyntheseAnneeDTO(2026, false, 15_000_000d, 4_200_000d, 31.8, 10_000d, 1_500d, 150_000d, 4.2)
            ),
            familles,
            null
        );
    }

    private static SpringTemplateEngine lireMoteur() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setTemplateMode("HTML");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
