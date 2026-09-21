package com.kobe.warehouse.service.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.service.dto.mobile.MobileTvaReportDTO;
import com.kobe.warehouse.service.dto.mobile.TvaChartDataDTO;
import com.kobe.warehouse.service.dto.mobile.TvaRateBreakdownDTO;
import com.kobe.warehouse.service.financiel_transaction.TaxeService;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.financiel_transaction.dto.TaxeDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TaxeWrapperDTO;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * L'état de TVA de l'application mobile reprend celui du poste fixe et le remet en forme pour un
 * écran de téléphone. Il n'invente aucun chiffre : il ventile, totalise par taux et calcule des
 * parts pour le graphique.
 *
 * <p>C'est un document que le pharmacien consulte pour sa déclaration mensuelle, et deux choses y
 * décident de sa justesse. Le <b>périmètre</b> : la TVA se déclare sur le chiffre d'affaires, ventes
 * dépôt comprises — en omettre une catégorie fausse la déclaration. Et la <b>ventilation par
 * taux</b> : deux jours au même taux doivent se cumuler sur une seule ligne quand on ne détaille
 * pas par date, et rester séparés quand on le demande.
 *
 * <p>Le libellé de période compte aussi, modestement : sur un téléphone, « Aujourd'hui » se lit
 * mieux qu'une date, et confondre les deux fait douter de ce qu'on regarde.
 */
@DisplayName("MobileTvaReportService — état de TVA sur mobile")
class MobileTvaReportServiceTest {

    private final TaxeService taxeService = mock(TaxeService.class);
    private final MobileTvaReportService service = new MobileTvaReportService(taxeService);

    @BeforeEach
    void setUp() {
        when(taxeService.fetchTaxe(any(), anyBoolean())).thenReturn(wrapper(0, List.of()));
    }

    // ===== périmètre interrogé =====

    @Nested
    @DisplayName("Périmètre interrogé")
    class PerimetreInterroge {

        /** La TVA se déclare sur le chiffre d'affaires, ventes dépôt comprises. */
        @Test
        @DisplayName("les deux catégories de chiffre d'affaires sont demandées")
        void deuxCategories() {
            service.getTvaReport(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), false);

            assertThat(parametresInterroges().getCategorieChiffreAffaires())
                .containsExactlyInAnyOrder(CategorieChiffreAffaire.CA, CategorieChiffreAffaire.CA_DEPOT);
        }

        @Test
        @DisplayName("la période demandée est transmise telle quelle")
        void periodeTransmise() {
            service.getTvaReport(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), false);

            MvtParam params = parametresInterroges();

            assertThat(params.getFromDate()).isEqualTo(LocalDate.of(2026, 3, 1));
            assertThat(params.getToDate()).isEqualTo(LocalDate.of(2026, 3, 31));
        }

        /** L'écran mobile consulte souvent une seule journée : il n'envoie alors qu'une date. */
        @Test
        @DisplayName("sans date de fin, la journée demandée sert de période")
        void sansDateDeFin() {
            LocalDate jour = LocalDate.of(2026, 3, 15);

            MobileTvaReportDTO rapport = service.getTvaReport(jour, null, false);

            assertThat(parametresInterroges().getToDate()).isEqualTo(jour);
            assertThat(rapport.toDate()).isEqualTo(jour);
        }

        @Test
        @DisplayName("le détail par date n'est demandé que s'il est voulu")
        void detailParDate() {
            service.getTvaReport(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), true);
            assertThat(parametresInterroges().getGroupeBy()).isEqualTo("daily");

            service.getTvaReport(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), false);
            assertThat(parametresInterroges().getGroupeBy()).isNull();
        }
    }

    // ===== ventilation par taux =====

    @Nested
    @DisplayName("Ventilation par taux")
    class VentilationParTaux {

        /** Sans détail par date, les journées d'un même taux se cumulent sur une seule ligne. */
        @Test
        @DisplayName("les journées d'un même taux se cumulent")
        void cumulParTaux() {
            when(taxeService.fetchTaxe(any(), anyBoolean())).thenReturn(
                wrapper(
                    3_000_000,
                    List.of(
                        taxe(18, LocalDate.of(2026, 3, 1), 1_000_000, 180_000, 1_180_000),
                        taxe(18, LocalDate.of(2026, 3, 2), 500_000, 90_000, 590_000),
                        taxe(0, LocalDate.of(2026, 3, 1), 1_230_000, 0, 1_230_000)
                    )
                )
            );

            List<TvaRateBreakdownDTO> ventilation = rapport(false).tvaBreakdown();

            assertThat(ventilation).hasSize(2);
            assertThat(ventilation.get(1).codeTva()).isEqualTo(18);
            assertThat(ventilation.get(1).montantHt()).isEqualTo(1_500_000);
            assertThat(ventilation.get(1).montantTva()).isEqualTo(270_000);
            assertThat(ventilation.get(1).montantTtc()).isEqualTo(1_770_000);
        }

        /** Les taux sont présentés du plus bas au plus haut : c'est l'ordre d'un état de TVA. */
        @Test
        @DisplayName("les taux sont présentés dans l'ordre croissant")
        void ordreCroissantDesTaux() {
            when(taxeService.fetchTaxe(any(), anyBoolean())).thenReturn(
                wrapper(
                    1_000_000,
                    List.of(taxe(18, null, 100, 18, 118), taxe(0, null, 100, 0, 100), taxe(9, null, 100, 9, 109))
                )
            );

            assertThat(rapport(false).tvaBreakdown()).extracting(TvaRateBreakdownDTO::codeTva).containsExactly(0, 9, 18);
        }

        @Test
        @DisplayName("un taux absent est traité comme zéro plutôt que d'écarter la ligne")
        void tauxAbsent() {
            when(taxeService.fetchTaxe(any(), anyBoolean())).thenReturn(
                wrapper(1_000_000, List.of(taxe(null, null, 500_000, 0, 500_000)))
            );

            assertThat(rapport(false).tvaBreakdown()).singleElement().satisfies(ligne -> {
                assertThat(ligne.codeTva()).isZero();
                assertThat(ligne.rateName()).isEqualTo("0%");
                assertThat(ligne.montantHt()).isEqualTo(500_000);
            });
        }

        /** Avec le détail par date, chaque journée garde sa ligne — c'est ce qui permet de pointer. */
        @Test
        @DisplayName("le détail par date conserve une ligne par jour et par taux")
        void detailParDate() {
            when(taxeService.fetchTaxe(any(), anyBoolean())).thenReturn(
                wrapper(
                    3_000_000,
                    List.of(
                        taxe(18, LocalDate.of(2026, 3, 2), 500_000, 90_000, 590_000),
                        taxe(18, LocalDate.of(2026, 3, 1), 1_000_000, 180_000, 1_180_000)
                    )
                )
            );

            List<TvaRateBreakdownDTO> ventilation = rapport(true).tvaBreakdown();

            assertThat(ventilation).hasSize(2);
            assertThat(ventilation).extracting(TvaRateBreakdownDTO::date)
                .containsExactly(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 2));
        }

        @Test
        @DisplayName("sans détail par date, les lignes ne portent pas de date")
        void sansDateHorsDetail() {
            when(taxeService.fetchTaxe(any(), anyBoolean())).thenReturn(
                wrapper(1_000_000, List.of(taxe(18, LocalDate.of(2026, 3, 1), 100, 18, 118)))
            );

            assertThat(rapport(false).tvaBreakdown()).singleElement().extracting(TvaRateBreakdownDTO::date).isNull();
        }
    }

    // ===== graphique =====

    @Nested
    @DisplayName("Graphique")
    class Graphique {

        @Test
        @DisplayName("chaque taux occupe une part du total toutes taxes comprises")
        void partsDuTotal() {
            when(taxeService.fetchTaxe(any(), anyBoolean())).thenReturn(
                wrapper(
                    1_000_000,
                    List.of(taxe(18, null, 600_000, 108_000, 750_000), taxe(0, null, 250_000, 0, 250_000))
                )
            );

            List<TvaChartDataDTO> graphique = rapport(false).chartData();

            assertThat(graphique).extracting(TvaChartDataDTO::label).containsExactly("0%", "18%");
            assertThat(graphique).extracting(TvaChartDataDTO::percent).containsExactly(25.0, 75.0);
        }

        @Test
        @DisplayName("les parts sont arrondies au dixième")
        void arrondiAuDixieme() {
            when(taxeService.fetchTaxe(any(), anyBoolean())).thenReturn(
                wrapper(3_000, List.of(taxe(18, null, 1_000, 0, 1_000)))
            );

            assertThat(rapport(false).chartData()).singleElement().extracting(TvaChartDataDTO::percent).isEqualTo(33.3);
        }

        /** Un total nul ne se divise pas : la part vaut zéro plutôt qu'un infini que l'écran afficherait mal. */
        @Test
        @DisplayName("un total nul ne provoque pas de division par zéro")
        void totalNul() {
            when(taxeService.fetchTaxe(any(), anyBoolean())).thenReturn(wrapper(0, List.of(taxe(18, null, 0, 0, 0))));

            assertThat(rapport(false).chartData()).singleElement().extracting(TvaChartDataDTO::percent).isEqualTo(0.0);
        }

        @Test
        @DisplayName("chaque taux reçoit sa propre couleur")
        void couleursDistinctes() {
            when(taxeService.fetchTaxe(any(), anyBoolean())).thenReturn(
                wrapper(
                    1_000_000,
                    List.of(taxe(0, null, 100, 0, 300_000), taxe(9, null, 100, 9, 300_000), taxe(18, null, 100, 18, 400_000))
                )
            );

            assertThat(rapport(false).chartData()).extracting(TvaChartDataDTO::color).doesNotHaveDuplicates();
        }

        /** Avec le détail par date, le graphique regroupe tout de même par taux : il n'a que faire des jours. */
        @Test
        @DisplayName("le graphique regroupe par taux, même quand le détail est journalier")
        void graphiqueRegroupeParTaux() {
            when(taxeService.fetchTaxe(any(), anyBoolean())).thenReturn(
                wrapper(
                    1_000_000,
                    List.of(
                        taxe(18, LocalDate.of(2026, 3, 1), 100, 18, 600_000),
                        taxe(18, LocalDate.of(2026, 3, 2), 100, 18, 400_000)
                    )
                )
            );

            assertThat(rapport(true).chartData()).singleElement().satisfies(part -> {
                assertThat(part.label()).isEqualTo("18%");
                assertThat(part.value()).isEqualTo(1_000_000);
                assertThat(part.percent()).isEqualTo(100.0);
            });
        }
    }

    // ===== libellé de période =====

    @Nested
    @DisplayName("Libellé de période")
    class LibelleDePeriode {

        /** Sur un téléphone, « Aujourd'hui » se lit mieux qu'une date — et lève l'ambiguïté. */
        @Test
        @DisplayName("la journée en cours se nomme « Aujourd'hui »")
        void aujourdHui() {
            assertThat(service.getTvaReport(LocalDate.now(), LocalDate.now(), false).periodLabel()).isEqualTo("Aujourd'hui");
        }

        @Test
        @DisplayName("la veille se nomme « Hier »")
        void hier() {
            LocalDate hier = LocalDate.now().minusDays(1);

            assertThat(service.getTvaReport(hier, hier, false).periodLabel()).isEqualTo("Hier");
        }

        @Test
        @DisplayName("une autre journée s'affiche à sa date")
        void autreJournee() {
            LocalDate jour = LocalDate.of(2026, 3, 15);

            assertThat(service.getTvaReport(jour, jour, false).periodLabel()).isEqualTo("15/03/2026");
        }

        @Test
        @DisplayName("une période s'affiche par ses deux bornes")
        void periode() {
            assertThat(service.getTvaReport(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), false).periodLabel())
                .isEqualTo("01/03/2026 - 31/03/2026");
        }
    }

    // ===== période sans mouvement =====

    @Nested
    @DisplayName("Période sans mouvement")
    class PeriodeSansMouvement {

        @Test
        @DisplayName("une période sans taxe rend un état vide mais daté")
        void aucuneTaxe() {
            MobileTvaReportDTO rapport = service.getTvaReport(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), false);

            assertThat(rapport.montantTtc()).isZero();
            assertThat(rapport.tvaBreakdown()).isEmpty();
            assertThat(rapport.chartData()).isEmpty();
            assertThat(rapport.periodLabel()).isEqualTo("01/03/2026 - 31/03/2026");
            assertThat(rapport.fromDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        }

        /** Le service sous-jacent peut ne rien rendre du tout : l'écran doit s'ouvrir quand même. */
        @Test
        @DisplayName("une réponse absente du service sous-jacent rend un état vide")
        void reponseAbsente() {
            when(taxeService.fetchTaxe(any(), anyBoolean())).thenReturn(null);

            MobileTvaReportDTO rapport = service.getTvaReport(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), false);

            assertThat(rapport.tvaBreakdown()).isEmpty();
            assertThat(rapport.montantTtc()).isZero();
        }
    }

    // ===== totaux =====

    @Test
    @DisplayName("les totaux de l'état reprennent ceux du service sous-jacent")
    void totauxRepris() {
        TaxeWrapperDTO source = wrapper(1_180_000, List.of(taxe(18, null, 1_000_000, 180_000, 1_180_000)));
        source.setMontantHt(1_000_000);
        source.setMontantTaxe(180_000);
        source.setMontantNet(1_150_000);
        source.setMontantRemise(30_000);
        source.setMontantAchat(700_000);
        when(taxeService.fetchTaxe(any(), anyBoolean())).thenReturn(source);

        MobileTvaReportDTO rapport = rapport(false);

        assertThat(rapport.montantHt()).isEqualTo(1_000_000);
        assertThat(rapport.montantTva()).isEqualTo(180_000);
        assertThat(rapport.montantTtc()).isEqualTo(1_180_000);
        assertThat(rapport.montantNet()).isEqualTo(1_150_000);
        assertThat(rapport.montantRemise()).isEqualTo(30_000);
        assertThat(rapport.montantAchat()).isEqualTo(700_000);
    }

    // ===== fabriques =====

    private MobileTvaReportDTO rapport(boolean detailParDate) {
        return service.getTvaReport(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), detailParDate);
    }

    private MvtParam parametresInterroges() {
        ArgumentCaptor<MvtParam> params = ArgumentCaptor.forClass(MvtParam.class);
        verify(taxeService, org.mockito.Mockito.atLeastOnce()).fetchTaxe(params.capture(), anyBoolean());
        return params.getValue();
    }

    private static TaxeWrapperDTO wrapper(long montantTtc, List<TaxeDTO> taxes) {
        TaxeWrapperDTO wrapper = new TaxeWrapperDTO();
        wrapper.setMontantTtc(montantTtc);
        wrapper.setTaxes(taxes);
        return wrapper;
    }

    private static TaxeDTO taxe(Integer codeTva, LocalDate date, long montantHt, long montantTaxe, long montantTtc) {
        TaxeDTO taxe = new TaxeDTO();
        taxe.setCodeTva(codeTva);
        taxe.setMvtDate(date);
        taxe.setMontantHt(montantHt);
        taxe.setMontantTaxe(montantTaxe);
        taxe.setMontantTtc(montantTtc);
        taxe.setMontantAchat(0L);
        taxe.setAmountToBeTakenIntoAccount(0L);
        return taxe;
    }
}
