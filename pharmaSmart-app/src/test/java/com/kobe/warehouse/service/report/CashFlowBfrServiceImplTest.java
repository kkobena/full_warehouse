package com.kobe.warehouse.service.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.repository.CashFlowBfrRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.report.BfrEvolutionDTO;
import com.kobe.warehouse.service.dto.report.BfrSnapshotDTO;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Le besoin en fonds de roulement dit combien d'argent l'officine immobilise en permanence : ce
 * qu'elle a en rayon, plus ce que les tiers payants lui doivent, moins ce qu'elle doit à ses
 * grossistes.
 *
 * <p>Les trois délais qui l'expliquent — stock, créances, dettes — sont des <b>rapports à un flux
 * annuel</b> : tant de jours de stock, c'est la valeur du stock rapportée au coût d'achat écoulé en
 * un an. Une officine qui vient d'ouvrir, ou un exercice sans achat, met donc un zéro au
 * dénominateur ; le tableau doit alors afficher zéro et non faire échouer l'écran.
 */
@DisplayName("CashFlowBfrService — besoin en fonds de roulement")
class CashFlowBfrServiceImplTest {

    private static final int MAGASIN = 3;

    // Doublures créées à l'initialisation des champs, et non dans un @BeforeEach : le délai de
    // quinze secondes qui borne les méthodes de cycle de vie coupait l'amorçage de Mockito.
    private final CashFlowBfrRepository repository = mock(CashFlowBfrRepository.class);
    private final UserService userService = mock(UserService.class);

    private final CashFlowBfrServiceImpl service = new CashFlowBfrServiceImpl(repository, userService);

    @BeforeEach
    void officineAuRepos() {
        Magasin magasin = new Magasin();
        ReflectionTestUtils.setField(magasin, "id", MAGASIN);
        AppUser utilisateur = new AppUser();
        utilisateur.setMagasin(magasin);
        when(userService.getUser()).thenReturn(utilisateur);

        when(repository.getStockValue(MAGASIN)).thenReturn(0L);
        when(repository.getCreanceTp()).thenReturn(0L);
        when(repository.getDetteFournisseur()).thenReturn(0L);
        when(repository.getCogs12m()).thenReturn(0L);
        when(repository.getCaTp12m()).thenReturn(0L);
        when(repository.getAchats12m()).thenReturn(0L);
        when(repository.findEvolution()).thenReturn(List.of());
    }

    // ===== besoin en fonds de roulement =====

    @Nested
    @DisplayName("Besoin en fonds de roulement")
    class BesoinEnFondsDeRoulement {

        @Test
        @DisplayName("le stock et les créances l'alimentent, les dettes fournisseurs l'allègent")
        void compositionDuBfr() {
            when(repository.getStockValue(MAGASIN)).thenReturn(30_000_000L);
            when(repository.getCreanceTp()).thenReturn(12_000_000L);
            when(repository.getDetteFournisseur()).thenReturn(8_000_000L);

            BfrSnapshotDTO instantane = service.getSnapshot();

            assertThat(instantane.stockValue()).isEqualTo(30_000_000L);
            assertThat(instantane.creancesTp()).isEqualTo(12_000_000L);
            assertThat(instantane.dettesFournisseurs()).isEqualTo(8_000_000L);
            assertThat(instantane.bfr()).isEqualTo(34_000_000L);
        }

        /**
         * Une avance nette au grossiste rendrait une dette négative, qui <i>augmenterait</i> le
         * besoin en fonds de roulement au lieu de le réduire. Elle est ramenée à zéro.
         */
        @Test
        @DisplayName("une dette fournisseur négative est ramenée à zéro")
        void detteNegative() {
            when(repository.getStockValue(MAGASIN)).thenReturn(30_000_000L);
            when(repository.getDetteFournisseur()).thenReturn(-500_000L);

            BfrSnapshotDTO instantane = service.getSnapshot();

            assertThat(instantane.dettesFournisseurs()).isZero();
            assertThat(instantane.bfr()).isEqualTo(30_000_000L);
        }

        @Test
        @DisplayName("des dettes supérieures à l'actif circulant rendent un besoin négatif")
        void bfrNegatif() {
            when(repository.getStockValue(MAGASIN)).thenReturn(5_000_000L);
            when(repository.getDetteFournisseur()).thenReturn(9_000_000L);

            assertThat(service.getSnapshot().bfr()).isEqualTo(-4_000_000L);
        }

        @Test
        @DisplayName("l'instantané porte sur le magasin de l'utilisateur connecté")
        void magasinDeLUtilisateur() {
            service.getSnapshot();

            org.mockito.Mockito.verify(repository).getStockValue(MAGASIN);
        }
    }

    // ===== délais =====

    @Nested
    @DisplayName("Délais de rotation")
    class DelaisDeRotation {

        /** Trente millions de stock pour un coût d'achat annuel de cent quatre-vingts : soixante jours. */
        @Test
        @DisplayName("le délai de stock rapporte le stock au coût d'achat annuel")
        void delaiDeStock() {
            when(repository.getStockValue(MAGASIN)).thenReturn(30_000_000L);
            when(repository.getCogs12m()).thenReturn(182_500_000L);

            assertThat(service.getSnapshot().dio()).isEqualTo(60);
        }

        @Test
        @DisplayName("le délai de créance rapporte l'encours au facturé annuel")
        void delaiDeCreance() {
            when(repository.getCreanceTp()).thenReturn(12_000_000L);
            when(repository.getCaTp12m()).thenReturn(73_000_000L);

            assertThat(service.getSnapshot().dso()).isEqualTo(60);
        }

        @Test
        @DisplayName("le délai de dette rapporte la dette aux achats annuels")
        void delaiDeDette() {
            when(repository.getDetteFournisseur()).thenReturn(8_000_000L);
            when(repository.getAchats12m()).thenReturn(97_333_333L);

            assertThat(service.getSnapshot().dpo()).isEqualTo(30);
        }

        /** Le cycle de trésorerie : le temps pendant lequel l'argent est sorti sans être rentré. */
        @Test
        @DisplayName("le cycle de trésorerie ajoute stock et créances, retranche les dettes")
        void cycleDeTresorerie() {
            when(repository.getStockValue(MAGASIN)).thenReturn(30_000_000L);
            when(repository.getCogs12m()).thenReturn(182_500_000L);
            when(repository.getCreanceTp()).thenReturn(12_000_000L);
            when(repository.getCaTp12m()).thenReturn(73_000_000L);
            when(repository.getDetteFournisseur()).thenReturn(8_000_000L);
            when(repository.getAchats12m()).thenReturn(97_333_333L);

            BfrSnapshotDTO instantane = service.getSnapshot();

            assertThat(instantane.ccc()).isEqualTo(instantane.dio() + instantane.dso() - instantane.dpo());
            assertThat(instantane.ccc()).isEqualTo(90);
        }

        /** Une officine qui n'a encore rien vendu : le délai n'a pas de sens, il vaut zéro. */
        @Test
        @DisplayName("un flux annuel nul rend un délai nul plutôt qu'une division par zéro")
        void fluxAnnuelNul() {
            when(repository.getStockValue(MAGASIN)).thenReturn(30_000_000L);
            when(repository.getCreanceTp()).thenReturn(12_000_000L);
            when(repository.getDetteFournisseur()).thenReturn(8_000_000L);

            BfrSnapshotDTO instantane = service.getSnapshot();

            assertThat(instantane.dio()).isZero();
            assertThat(instantane.dso()).isZero();
            assertThat(instantane.dpo()).isZero();
            assertThat(instantane.ccc()).isZero();
        }

        @Test
        @DisplayName("le délai est arrondi au jour le plus proche")
        void arrondiAuJour() {
            when(repository.getStockValue(MAGASIN)).thenReturn(1_500_000L);
            when(repository.getCogs12m()).thenReturn(365_000_000L);

            // 1 500 000 / (365 000 000 / 365) = 1,5 jour
            assertThat(service.getSnapshot().dio()).isEqualTo(2);
        }

        @Test
        @DisplayName("une officine au repos affiche des délais nuls sans échouer")
        void officineAuRepos() {
            BfrSnapshotDTO instantane = service.getSnapshot();

            assertThat(instantane.bfr()).isZero();
            assertThat(instantane.ccc()).isZero();
        }
    }

    // ===== évolution =====

    @Nested
    @DisplayName("Évolution sur douze mois")
    class Evolution {

        @Test
        @DisplayName("chaque mois porte son libellé, ses créances émises et ses achats reçus")
        void contenuDeLaCourbe() {
            when(repository.findEvolution()).thenReturn(
                List.<Object[]>of(new Object[] { 2026, 1, 5_000_000L, 4_000_000L }, new Object[] { 2026, 2, 6_000_000L, 3_500_000L })
            );

            BfrEvolutionDTO evolution = service.getEvolution();

            assertThat(evolution.labels()).containsExactly(libelle(2026, 1), libelle(2026, 2));
            assertThat(evolution.creancesEmises()).containsExactly(5_000_000L, 6_000_000L);
            assertThat(evolution.achatsRecus()).containsExactly(4_000_000L, 3_500_000L);
        }

        @Test
        @DisplayName("un mois sans mouvement vaut zéro et ne disparaît pas de la courbe")
        void moisSansMouvement() {
            when(repository.findEvolution()).thenReturn(
                List.<Object[]>of(new Object[] { 2026, 1, null, null }, new Object[] { 2026, 2, 6_000_000L, 3_500_000L })
            );

            BfrEvolutionDTO evolution = service.getEvolution();

            assertThat(evolution.labels()).hasSize(2);
            assertThat(evolution.creancesEmises()).containsExactly(0L, 6_000_000L);
            assertThat(evolution.achatsRecus()).containsExactly(0L, 3_500_000L);
        }

        @Test
        @DisplayName("les trois séries ont toujours la même longueur")
        void seriesDeMemeLongueur() {
            when(repository.findEvolution()).thenReturn(
                List.<Object[]>of(new Object[] { 2025, 12, 1L, 1L }, new Object[] { 2026, 1, 2L, 2L }, new Object[] { 2026, 2, 3L, 3L })
            );

            BfrEvolutionDTO evolution = service.getEvolution();

            assertThat(evolution.creancesEmises()).hasSameSizeAs(evolution.labels());
            assertThat(evolution.achatsRecus()).hasSameSizeAs(evolution.labels());
        }

        @Test
        @DisplayName("aucune donnée rend une courbe vide")
        void aucuneDonnee() {
            BfrEvolutionDTO evolution = service.getEvolution();

            assertThat(evolution.labels()).isEmpty();
            assertThat(evolution.creancesEmises()).isEmpty();
        }
    }

    // ===== utilitaires =====

    private static String libelle(int annee, int mois) {
        return YearMonth.of(annee, mois).atDay(1).format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH));
    }
}
