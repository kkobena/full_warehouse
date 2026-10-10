package com.kobe.warehouse.service.pilotage.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.enumeration.Granularite;
import com.kobe.warehouse.domain.enumeration.TypeComparaison;
import com.kobe.warehouse.repository.PilotageAchatsStockRepository;
import com.kobe.warehouse.repository.PilotageMesuresRepository;
import com.kobe.warehouse.repository.PilotageRupturesRepository;
import com.kobe.warehouse.service.dto.pilotage.AchatsPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.AchatsVentesPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.ComptageDTO;
import com.kobe.warehouse.service.dto.pilotage.DelaiFournisseurDTO;
import com.kobe.warehouse.service.dto.pilotage.FournisseurAchatsDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneAchatsDTO;
import com.kobe.warehouse.service.dto.pilotage.LignesJourDTO;
import com.kobe.warehouse.service.dto.pilotage.MoisStockDTO;
import com.kobe.warehouse.service.dto.pilotage.MontantJourDTO;
import com.kobe.warehouse.service.dto.pilotage.MontantsFamilleDTO;
import com.kobe.warehouse.service.dto.pilotage.PeremptionMoisDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.dto.pilotage.PointAchatsVentesDTO;
import com.kobe.warehouse.service.dto.pilotage.PointStockDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.RupturesPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.StockFamilleDTO;
import com.kobe.warehouse.service.dto.pilotage.StockPilotageDTO;
import com.kobe.warehouse.service.pilotage.PeriodeComparaisonService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Pilotage — achats & stock")
class AchatsStockPilotageTest {

    private static final PeriodeDTO SEPTEMBRE = new PeriodeDTO(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
    private static final PeriodeDTO SEPTEMBRE_N1 = new PeriodeDTO(LocalDate.of(2025, 9, 1), LocalDate.of(2025, 9, 30));
    private static final RequetePilotageDTO REQUETE = new RequetePilotageDTO(SEPTEMBRE.du(), SEPTEMBRE.au(), TypeComparaison.MEME_PERIODE_N_1, null, null, null, false, Granularite.SEMAINE, null);

    @Mock
    private PeriodeComparaisonService periodeComparaisonService;

    @Mock
    private PilotageAchatsStockRepository achatsStock;

    @Mock
    private PilotageMesuresRepository mesures;

    @Mock
    private PilotageRupturesRepository ruptures;

    @Mock
    private AppConfigurationService configuration;

    @BeforeEach
    void comparerSeptembreN1() {
        when(periodeComparaisonService.comparer(any(), any())).thenReturn(new ComparaisonPeriodesDTO(SEPTEMBRE, SEPTEMBRE_N1, false));
    }

    @Nested
    @DisplayName("achats")
    class Achats {

        private AchatsPilotageServiceImpl service;

        @BeforeEach
        void deuxFournisseurs() {
            service = new AchatsPilotageServiceImpl(periodeComparaisonService, achatsStock, mesures);
            when(achatsStock.sommerAchatsParFournisseur(eq(SEPTEMBRE.du()), any(), anyList())).thenReturn(
                List.of(new FournisseurAchatsDTO("1", "Copharmed", 60_000L, 3L, 100L, 90L), new FournisseurAchatsDTO("2", "Laborex", 40_000L, 1L, 50L, 50L))
            );
            when(achatsStock.sommerAchatsParFournisseur(eq(SEPTEMBRE_N1.du()), any(), anyList())).thenReturn(List.of(new FournisseurAchatsDTO("1", "Copharmed", 50_000L, 2L, 10L, 10L)));
            // 3 bons à 2 jours, 1 bon à 6 jours : 3 jours en moyenne, chaque fournisseur pesant ses bons.
            when(achatsStock.listerDelaisParFournisseur(eq(SEPTEMBRE.du()), any(), anyList())).thenReturn(
                List.of(new DelaiFournisseurDTO("1", 2.0, 3L), new DelaiFournisseurDTO("2", 6.0, 1L))
            );
            when(achatsStock.sommerAchatsParFamille(eq(SEPTEMBRE.du()), any())).thenReturn(List.of(new MontantsFamilleDTO("9", "Antalgiques", 90_000L, 100_000L)));
            when(achatsStock.sommerAchatsParFamille(eq(SEPTEMBRE_N1.du()), any())).thenReturn(List.of(new MontantsFamilleDTO("9", "Antalgiques", 45_000L, 50_000L)));
        }

        @Test
        @DisplayName("délai moyen pondéré par les bons, conformité des quantités, part et variation par fournisseur")
        void tuilesEtFournisseurs() {
            AchatsPilotageDTO achats = service.analyserAchats(REQUETE);

            assertThat(achats.achatsTtc().valeur()).isEqualTo(100_000);
            assertThat(achats.achatsTtc().ecartPct()).isCloseTo(100.0, within(1e-9));
            assertThat(achats.nbBons().valeur()).isEqualTo(4);
            assertThat(achats.delaiMoyen().valeur()).isCloseTo(3.0, within(1e-9));
            assertThat(achats.conformite().valeur()).isCloseTo(140 * 100.0 / 150, within(1e-9));
            assertThat(achats.fournisseurs())
                .extracting(LigneAchatsDTO::libelle, LigneAchatsDTO::part, ligne -> ligne.montant().valeurReference())
                .containsExactly(tuple("Copharmed", 60.0, 50_000.0), tuple("Laborex", 40.0, 0.0));
        }

        @Test
        @DisplayName("achats et coût des ventes rangés dans les tranches")
        void achatsVentes() {
            when(achatsStock.sommerAchatsHtParJour(any(), any())).thenReturn(
                List.of(new MontantJourDTO(LocalDate.of(2026, 9, 2), 1_000L), new MontantJourDTO(LocalDate.of(2026, 9, 9), 500L))
            );
            when(mesures.listerLignesParJour(any(), any(), any())).thenReturn(List.of(new LignesJourDTO(LocalDate.of(2026, 9, 3), 2_000L, 1_800L, 700L, 4L)));
            when(achatsStock.sommerCoutVentesParFamille(any(), any(), any())).thenReturn(List.of(new MontantsFamilleDTO("9", "Antalgiques", 600L, 700L)));

            AchatsVentesPilotageDTO achatsVentes = service.analyserAchatsVentes(REQUETE);

            assertThat(achatsVentes.points().getFirst()).extracting(PointAchatsVentesDTO::achatsHt, PointAchatsVentesDTO::coutVentesHt).containsExactly(1_000L, 700L);
            assertThat(achatsVentes.points().get(1).achatsHt()).isEqualTo(500);
            assertThat(achatsVentes.familles().getFirst().ecart()).isEqualTo(90_000 - 600);
        }
    }

    @Nested
    @DisplayName("stock et ruptures")
    class Stock {

        private StockPilotageServiceImpl service;

        @BeforeEach
        void creer() {
            service = new StockPilotageServiceImpl(periodeComparaisonService, achatsStock, ruptures, configuration);
        }

        @Test
        @DisplayName("valeur fin de mois face à N-1 ; rotation et couverture sur le coût des ventes de 12 mois ; courbe")
        void stock() {
            LocalDate septembre = LocalDate.of(2026, 9, 30);
            when(configuration.getSeuilStockDormantPilotage()).thenReturn(90);
            when(achatsStock.sommerStockParMois(any(), eq(septembre))).thenReturn(
                List.of(new MoisStockDTO(LocalDate.of(2025, 9, 30), 80_000L), new MoisStockDTO(septembre, 100_000L))
            );
            when(achatsStock.sommerCoutVentesTtc(eq(LocalDate.of(2025, 10, 1)), eq(septembre), any())).thenReturn(730_000L);
            when(achatsStock.sommerStockParFamille(septembre)).thenReturn(List.of(new StockFamilleDTO("9", "Antalgiques", 500L, 100_000L)));
            when(achatsStock.sommerDormantsParFamille(eq(septembre), any())).thenReturn(List.of(new StockFamilleDTO("9", "Antalgiques", 4L, 12_000L)));

            StockPilotageDTO stock = service.analyserStock(REQUETE);

            assertThat(stock.valeur().valeur()).isEqualTo(100_000);
            assertThat(stock.valeur().valeurReference()).isEqualTo(80_000);
            assertThat(stock.rotation()).isCloseTo(7.3, within(1e-9));
            assertThat(stock.couvertureJours()).isCloseTo(50.0, within(1e-9));
            assertThat(stock.nbDormants()).isEqualTo(4);
            assertThat(stock.valeurDormante()).isEqualTo(12_000);
            assertThat(stock.courbe()).hasSize(12);
            assertThat(stock.courbe().getLast()).extracting(PointStockDTO::valeur, PointStockDTO::valeurN1).containsExactly(100_000L, 80_000L);
        }

        @Test
        @DisplayName("taux de rupture fournisseurs et taux de ventes manquées : deux mesures distinctes")
        void ruptures() {
            when(ruptures.compterLignesCommandees(any(), any())).thenReturn(200L);
            when(ruptures.compterRuptures(any(), any())).thenReturn(10L);
            when(ruptures.sommerVentesManquees(any(), any())).thenReturn(new ComptageDTO("", "Total", 3L, 5L, 7_500L));
            when(ruptures.sommerQuantitesEnAvoir(any(), any(), any())).thenReturn(new ComptageDTO("", "Total", 1_000L, 20L, 0L));
            when(ruptures.sommerPeremptionsParMois(any(), any())).thenReturn(List.of(new PeremptionMoisDTO(2026, 9, 2L, 3_000L)));

            RupturesPilotageDTO resultat = service.analyserRuptures(REQUETE);

            assertThat(resultat.tauxRupture().valeur()).isCloseTo(5.0, within(1e-9));
            assertThat(resultat.ventesManquees().valeur()).isEqualTo(7_500);
            assertThat(resultat.tauxVentesManquees().valeur()).isCloseTo(2.0, within(1e-9));
            assertThat(resultat.valeurPerimee().valeur()).isEqualTo(3_000);
        }
    }
}
