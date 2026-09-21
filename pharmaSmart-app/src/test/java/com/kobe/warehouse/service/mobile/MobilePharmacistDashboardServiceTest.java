package com.kobe.warehouse.service.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.dto.mobile.ChartDataPointDTO;
import com.kobe.warehouse.service.dto.mobile.FournisseurAchatMobileDTO;
import com.kobe.warehouse.service.dto.mobile.MobilePharmacistDashboardDTO;
import com.kobe.warehouse.service.financiel_transaction.TableauPharmacienService;
import com.kobe.warehouse.service.financiel_transaction.dto.AchatDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.FournisseurAchat;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienWrapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Le tableau du pharmacien confronte ce qui est vendu à ce qui est acheté, sur la période demandée
 * et sur celle qui la précède immédiatement, pour en tirer une tendance.
 *
 * <p>Deux périodes, donc deux interrogations — et pas davantage : le calcul des variations
 * redemandait la période courante une fois par indicateur, ce qui portait à cinq le nombre de
 * requêtes pour un écran qu'on ouvre depuis un téléphone.
 */
@DisplayName("MobilePharmacistDashboardService — tableau du pharmacien")
class MobilePharmacistDashboardServiceTest {

    private static final LocalDate DEBUT = LocalDate.of(2026, 3, 10);
    private static final LocalDate FIN = LocalDate.of(2026, 3, 12);

    // Doublure créée à l'initialisation du champ, et non dans un @BeforeEach : le délai de quinze
    // secondes qui borne les méthodes de cycle de vie coupait l'amorçage de Mockito.
    private final TableauPharmacienService tableauPharmacienService = mock(TableauPharmacienService.class);

    private final MobilePharmacistDashboardService service = new MobilePharmacistDashboardService(tableauPharmacienService);

    // ===== périmètre =====

    @Nested
    @DisplayName("Périmètre interrogé")
    class PerimetreInterroge {

        @Test
        @DisplayName("seules les ventes clôturées, du chiffre d'affaires et des dépôts, sont retenues")
        void perimetreDuChiffreAffaires() {
            donneTableau(new TableauPharmacienWrapper());

            service.getPharmacistDashboard(DEBUT, FIN);

            MvtParam param = premierParametre();
            assertThat(param.getStatuts()).containsExactly(SalesStatut.CLOSED);
            assertThat(param.getCategorieChiffreAffaires())
                .containsExactlyInAnyOrder(CategorieChiffreAffaire.CA, CategorieChiffreAffaire.CA_DEPOT);
            assertThat(param.getGroupeBy()).isEqualTo("daily");
        }

        /**
         * Le défaut que ce test fixe : la période courante était redemandée une fois par variation,
         * soit cinq interrogations là où deux suffisent.
         */
        @Test
        @DisplayName("la période courante et la précédente sont interrogées une fois chacune")
        void deuxInterrogations() {
            donneTableau(new TableauPharmacienWrapper());

            service.getPharmacistDashboard(DEBUT, FIN);

            verify(tableauPharmacienService, times(2)).getTableauPharmacien(any());
        }

        @Test
        @DisplayName("la période précédente a la même durée et s'arrête la veille")
        void periodePrecedente() {
            donneTableau(new TableauPharmacienWrapper());

            service.getPharmacistDashboard(DEBUT, FIN);

            MvtParam precedent = parametres().get(1);
            assertThat(precedent.getFromDate()).isEqualTo(LocalDate.of(2026, 3, 7));
            assertThat(precedent.getToDate()).isEqualTo(LocalDate.of(2026, 3, 9));
        }

        @Test
        @DisplayName("les totaux du tableau sont repris tels quels")
        void totauxRepris() {
            donneTableau(
                new TableauPharmacienWrapper()
                    .setMontantVenteComptant(700_000L)
                    .setMontantVenteCredit(300_000L)
                    .setMontantVenteRemise(50_000L)
                    .setMontantVenteNet(1_000_000L)
                    .setMontantVenteTtc(1_050_000L)
                    .setMontantVenteHt(900_000L)
                    .setMontantVenteTaxe(150_000L)
                    .setNumberCount(42L)
                    .setMontantAchatNet(600_000L)
                    .setMontantAchatTtc(650_000L)
                    .setMontantAchatHt(550_000L)
                    .setMontantAchatTaxe(100_000L)
                    .setMontantAchatRemise(20_000L)
                    .setMontantAvoirFournisseur(15_000L)
                    .setRatioVenteAchat(1.67f)
                    .setRatioAchatVente(0.6f)
            );

            MobilePharmacistDashboardDTO resultat = service.getPharmacistDashboard(DEBUT, FIN);

            assertThat(resultat.montantVenteComptant()).isEqualTo(700_000L);
            assertThat(resultat.montantVenteCredit()).isEqualTo(300_000L);
            assertThat(resultat.montantVenteRemise()).isEqualTo(50_000L);
            assertThat(resultat.montantVenteNet()).isEqualTo(1_000_000L);
            assertThat(resultat.montantVenteTtc()).isEqualTo(1_050_000L);
            assertThat(resultat.montantVenteHt()).isEqualTo(900_000L);
            assertThat(resultat.montantVenteTaxe()).isEqualTo(150_000L);
            assertThat(resultat.transactionsCount()).isEqualTo(42);
            assertThat(resultat.montantAchatNet()).isEqualTo(600_000L);
            assertThat(resultat.montantAchatTtc()).isEqualTo(650_000L);
            assertThat(resultat.montantAchatHt()).isEqualTo(550_000L);
            assertThat(resultat.montantAchatTaxe()).isEqualTo(100_000L);
            assertThat(resultat.montantAchatRemise()).isEqualTo(20_000L);
            assertThat(resultat.montantAvoirFournisseur()).isEqualTo(15_000L);
        }
    }

    // ===== marge =====

    @Nested
    @DisplayName("Marge")
    class Marge {

        @Test
        @DisplayName("la marge est l'écart entre ventes nettes et achats nets")
        void margeEtTaux() {
            donneTableau(ventesEtAchats(1_000_000L, 600_000L));

            MobilePharmacistDashboardDTO resultat = service.getPharmacistDashboard(DEBUT, FIN);

            assertThat(resultat.marge()).isEqualTo(400_000L);
            assertThat(resultat.margePercent()).isEqualTo(40.0);
        }

        @Test
        @DisplayName("des achats supérieurs aux ventes donnent une marge négative")
        void margeNegative() {
            donneTableau(ventesEtAchats(500_000L, 800_000L));

            MobilePharmacistDashboardDTO resultat = service.getPharmacistDashboard(DEBUT, FIN);

            assertThat(resultat.marge()).isEqualTo(-300_000L);
            assertThat(resultat.margePercent()).isEqualTo(-60.0);
        }

        /** Sans vente, pas de division : le taux vaut zéro plutôt que de faire échouer l'écran. */
        @Test
        @DisplayName("une période sans vente rend un taux de marge nul")
        void aucuneVente() {
            donneTableau(ventesEtAchats(0L, 100_000L));

            assertThat(service.getPharmacistDashboard(DEBUT, FIN).margePercent()).isZero();
        }

        @Test
        @DisplayName("le taux de marge est arrondi au centième")
        void tauxArrondi() {
            donneTableau(ventesEtAchats(3_000_000L, 2_000_000L));

            assertThat(service.getPharmacistDashboard(DEBUT, FIN).margePercent()).isEqualTo(33.33);
        }
    }

    // ===== variations =====

    @Nested
    @DisplayName("Variation par rapport à la période précédente")
    class Variations {

        @Test
        @DisplayName("une progression des ventes se lit en pourcentage")
        void progression() {
            donneTableaux(ventesEtAchats(1_200_000L, 700_000L), ventesEtAchats(1_000_000L, 500_000L));

            MobilePharmacistDashboardDTO resultat = service.getPharmacistDashboard(DEBUT, FIN);

            assertThat(resultat.ventesVariation()).isEqualTo(20.0);
            assertThat(resultat.achatsVariation()).isEqualTo(40.0);
        }

        @Test
        @DisplayName("un recul se lit en pourcentage négatif")
        void recul() {
            donneTableaux(ventesEtAchats(750_000L, 0L), ventesEtAchats(1_000_000L, 0L));

            assertThat(service.getPharmacistDashboard(DEBUT, FIN).ventesVariation()).isEqualTo(-25.0);
        }

        /**
         * Une période précédente à zéro interdit tout ratio : le démarrage d'activité est conventionné
         * à cent pour cent, et l'absence des deux côtés à zéro.
         */
        @Test
        @DisplayName("un démarrage depuis une période vide compte pour cent pour cent")
        void demarrage() {
            donneTableaux(ventesEtAchats(500_000L, 0L), ventesEtAchats(0L, 0L));

            MobilePharmacistDashboardDTO resultat = service.getPharmacistDashboard(DEBUT, FIN);

            assertThat(resultat.ventesVariation()).isEqualTo(100.0);
            assertThat(resultat.achatsVariation()).isZero();
        }

        /** Une période précédente inaccessible ne doit pas priver le pharmacien du reste du tableau. */
        @Test
        @DisplayName("une période précédente inaccessible laisse la variation inconnue")
        void periodePrecedenteInaccessible() {
            when(tableauPharmacienService.getTableauPharmacien(any()))
                .thenReturn(ventesEtAchats(1_000_000L, 600_000L))
                .thenThrow(new IllegalStateException("base indisponible"));

            MobilePharmacistDashboardDTO resultat = service.getPharmacistDashboard(DEBUT, FIN);

            assertThat(resultat.ventesVariation()).isNull();
            assertThat(resultat.achatsVariation()).isNull();
            assertThat(resultat.marge()).isEqualTo(400_000L);
        }

        @Test
        @DisplayName("la variation est arrondie au centième")
        void variationArrondie() {
            donneTableaux(ventesEtAchats(1_000_000L, 0L), ventesEtAchats(300_000L, 0L));

            assertThat(service.getPharmacistDashboard(DEBUT, FIN).ventesVariation()).isEqualTo(233.33);
        }
    }

    // ===== fournisseurs =====

    @Nested
    @DisplayName("Palmarès des fournisseurs")
    class PalmaresDesFournisseurs {

        @Test
        @DisplayName("les fournisseurs sont classés du plus gros achat au plus petit")
        void classementDecroissant() {
            TableauPharmacienWrapper wrapper = ventesEtAchats(0L, 1_000_000L);
            wrapper.setGroupAchats(
                new ArrayList<>(List.of(fournisseur(1, "COPHARMED", 200_000L), fournisseur(2, "LABOREX", 800_000L)))
            );
            donneTableau(wrapper);

            assertThat(service.getPharmacistDashboard(DEBUT, FIN).topFournisseurs())
                .extracting(FournisseurAchatMobileDTO::libelle)
                .containsExactly("LABOREX", "COPHARMED");
        }

        @Test
        @DisplayName("chaque fournisseur porte sa part des achats de la période")
        void partDesAchats() {
            TableauPharmacienWrapper wrapper = ventesEtAchats(0L, 1_000_000L);
            wrapper.setGroupAchats(new ArrayList<>(List.of(fournisseur(1, "LABOREX", 800_000L))));
            donneTableau(wrapper);

            FournisseurAchatMobileDTO fournisseur = service.getPharmacistDashboard(DEBUT, FIN).topFournisseurs().getFirst();

            assertThat(fournisseur.id()).isEqualTo(1);
            assertThat(fournisseur.montantNet()).isEqualTo(800_000L);
            assertThat(fournisseur.percentTotal()).isEqualTo(80.0);
        }

        /** L'écran affiche un palmarès, pas un annuaire : dix lignes au plus. */
        @Test
        @DisplayName("le palmarès s'arrête à dix fournisseurs")
        void dixAuPlus() {
            TableauPharmacienWrapper wrapper = ventesEtAchats(0L, 1_000_000L);
            wrapper.setGroupAchats(
                IntStream.rangeClosed(1, 15)
                    .mapToObj(i -> fournisseur(i, "F" + i, i * 1_000L))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new))
            );
            donneTableau(wrapper);

            assertThat(service.getPharmacistDashboard(DEBUT, FIN).topFournisseurs())
                .hasSize(10)
                .first()
                .extracting(FournisseurAchatMobileDTO::libelle)
                .isEqualTo("F15");
        }

        @Test
        @DisplayName("aucun achat rend un palmarès vide")
        void aucunAchat() {
            donneTableau(new TableauPharmacienWrapper());

            assertThat(service.getPharmacistDashboard(DEBUT, FIN).topFournisseurs()).isEmpty();
        }
    }

    // ===== graphique =====

    @Nested
    @DisplayName("Graphique ventes / achats")
    class Graphique {

        @Test
        @DisplayName("chaque jour donne un point de vente et un point d'achat")
        void deuxPointsParJour() {
            TableauPharmacienWrapper wrapper = new TableauPharmacienWrapper();
            wrapper.setTableauPharmaciens(List.of(jour(LocalDate.of(2026, 3, 10), 500_000L, 300_000L)));
            donneTableau(wrapper);

            List<ChartDataPointDTO> points = service.getPharmacistDashboard(DEBUT, FIN).chartVentesAchats();

            assertThat(points).hasSize(2);
            assertThat(points.getFirst().type()).isEqualTo(ChartDataPointDTO.TYPE_SALES);
            assertThat(points.getFirst().value()).isEqualTo(500_000.0);
            assertThat(points.getLast().type()).isEqualTo(ChartDataPointDTO.TYPE_PURCHASES);
            assertThat(points.getLast().value()).isEqualTo(300_000.0);
        }

        @Test
        @DisplayName("les jours sont remis en ordre chronologique")
        void ordreChronologique() {
            TableauPharmacienWrapper wrapper = new TableauPharmacienWrapper();
            wrapper.setTableauPharmaciens(
                List.of(
                    jour(LocalDate.of(2026, 3, 12), 3L, 0L),
                    jour(LocalDate.of(2026, 3, 10), 1L, 0L),
                    jour(LocalDate.of(2026, 3, 11), 2L, 0L)
                )
            );
            donneTableau(wrapper);

            assertThat(service.getPharmacistDashboard(DEBUT, FIN).chartVentesAchats())
                .filteredOn(p -> ChartDataPointDTO.TYPE_SALES.equals(p.type()))
                .extracting(ChartDataPointDTO::value)
                .containsExactly(1.0, 2.0, 3.0);
        }

        @Test
        @DisplayName("le point porte le jour de la semaine abrégé, en français")
        void libelleDuJour() {
            TableauPharmacienWrapper wrapper = new TableauPharmacienWrapper();
            // Le 10 mars 2026 est un mardi.
            wrapper.setTableauPharmaciens(List.of(jour(LocalDate.of(2026, 3, 10), 1L, 0L)));
            donneTableau(wrapper);

            assertThat(service.getPharmacistDashboard(DEBUT, FIN).chartVentesAchats().getFirst().label())
                .startsWith("mar");
        }

        @Test
        @DisplayName("aucune donnée quotidienne rend un graphique vide")
        void aucunJour() {
            donneTableau(new TableauPharmacienWrapper());

            assertThat(service.getPharmacistDashboard(DEBUT, FIN).chartVentesAchats()).isEmpty();
        }
    }

    // ===== libellé de période =====

    @Nested
    @DisplayName("Libellé de période")
    class LibelleDePeriode {

        @Test
        @DisplayName("une journée unique s'affiche en clair")
        void journeeUnique() {
            assertThat(libellePour(DEBUT, DEBUT)).isEqualTo("10/03/2026");
        }

        @Test
        @DisplayName("une période dans le même mois se dit d'une traite")
        void memeMois() {
            assertThat(libellePour(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))).isEqualTo("Du 1 au 31 mars 2026");
        }

        @Test
        @DisplayName("une période à cheval sur deux mois affiche ses deux bornes")
        void deuxMois() {
            assertThat(libellePour(LocalDate.of(2026, 2, 25), LocalDate.of(2026, 3, 5))).isEqualTo("25/02/2026 - 05/03/2026");
        }

        /** Même mois, année différente : les bornes complètes lèvent l'ambiguïté. */
        @Test
        @DisplayName("un même mois d'une année sur l'autre affiche ses deux bornes")
        void memeMoisAnneeDifferente() {
            assertThat(libellePour(LocalDate.of(2025, 3, 1), LocalDate.of(2026, 3, 1))).isEqualTo("01/03/2025 - 01/03/2026");
        }

        private String libellePour(LocalDate debut, LocalDate fin) {
            donneTableau(new TableauPharmacienWrapper());
            return service.getPharmacistDashboard(debut, fin).periodLabel();
        }
    }

    // ===== utilitaires =====

    private void donneTableau(TableauPharmacienWrapper wrapper) {
        when(tableauPharmacienService.getTableauPharmacien(any())).thenReturn(wrapper);
    }

    /** La période courante d'abord, la précédente ensuite : c'est l'ordre des appels du service. */
    private void donneTableaux(TableauPharmacienWrapper courant, TableauPharmacienWrapper precedent) {
        when(tableauPharmacienService.getTableauPharmacien(any())).thenReturn(courant, precedent);
    }

    private List<MvtParam> parametres() {
        ArgumentCaptor<MvtParam> captor = ArgumentCaptor.forClass(MvtParam.class);
        verify(tableauPharmacienService, org.mockito.Mockito.atLeastOnce()).getTableauPharmacien(captor.capture());
        return captor.getAllValues();
    }

    private MvtParam premierParametre() {
        return parametres().getFirst();
    }

    private static TableauPharmacienWrapper ventesEtAchats(long venteNet, long achatNet) {
        return new TableauPharmacienWrapper().setMontantVenteNet(venteNet).setMontantAchatNet(achatNet);
    }

    private static FournisseurAchat fournisseur(int id, String libelle, long montantNet) {
        FournisseurAchat fournisseur = new FournisseurAchat();
        fournisseur.setId(id);
        fournisseur.setLibelle(libelle);
        fournisseur.setAchat(new AchatDTO().setMontantNet(montantNet).setMontantTtc(montantNet).setMontantHt(montantNet));
        return fournisseur;
    }

    private static TableauPharmacienDTO jour(LocalDate date, long venteNet, long bonAchat) {
        return new TableauPharmacienDTO().setMvtDate(date).setMontantNet(venteNet).setMontantBonAchat(bonAchat);
    }
}
