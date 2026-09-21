package com.kobe.warehouse.service.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.repository.VieillissementCreancesRepository;
import com.kobe.warehouse.service.dto.report.DsoOrganismeDTO;
import com.kobe.warehouse.service.dto.report.EncoursMensuelDTO;
import com.kobe.warehouse.service.dto.report.VieillissementGlobalDTO;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * Le vieillissement des créances range ce que les tiers payants doivent par ancienneté : ce qui vient
 * d'être facturé, ce qui traîne depuis un mois, deux, trois, et ce qui dépasse le trimestre.
 *
 * <p>Deux choses s'y jouent. La <b>lecture des colonnes</b> d'abord : la base rend un tableau brut
 * où l'encours total arrive en cinquième position, après les quatre tranches — un rang mal lu ne
 * lève aucune erreur, il affiche simplement un montant pour un autre. Le <b>verdict de fiabilité</b>
 * ensuite, qui décide de la couleur affichée en face de chaque organisme et donc de qui l'officine
 * va relancer en premier.
 */
@DisplayName("VieillissementCreancesService — vieillissement des créances")
class VieillissementCreancesServiceImplTest {

    // Doublure créée à l'initialisation du champ, et non dans un @BeforeEach : le délai de quinze
    // secondes qui borne les méthodes de cycle de vie coupait l'amorçage de Mockito.
    private final VieillissementCreancesRepository repository = mock(VieillissementCreancesRepository.class);

    private final VieillissementCreancesService service = new VieillissementCreancesServiceImpl(repository);

    @BeforeEach
    void aucuneCreance() {
        when(repository.findAgingGlobal()).thenReturn(new Object[] { 0L, 0L, 0L, 0L, 0L, 0L, 0L });
        when(repository.countAgingByOrganisme()).thenReturn(0L);
        when(repository.findAgingByOrganisme(anyInt(), anyInt())).thenReturn(List.of());
        when(repository.findEncoursMensuelEvolution()).thenReturn(List.of());
    }

    // ===== vue d'ensemble =====

    @Nested
    @DisplayName("Vue d'ensemble")
    class VueDEnsemble {

        /**
         * L'ordre des colonnes n'est pas celui du DTO : les quatre tranches précèdent l'encours
         * total. C'est ce décalage que ce test fixe.
         */
        @Test
        @DisplayName("chaque colonne du tableau brut atterrit dans le champ qui la désigne")
        void lectureDesColonnes() {
            when(repository.findAgingGlobal()).thenReturn(
                new Object[] { 1_000_000L, 2_000_000L, 3_000_000L, 4_000_000L, 10_000_000L, 42L, 17L }
            );

            VieillissementGlobalDTO global = service.getAgingGlobal();

            assertThat(global.tranche0_30()).isEqualTo(1_000_000L);
            assertThat(global.tranche31_60()).isEqualTo(2_000_000L);
            assertThat(global.tranche61_90()).isEqualTo(3_000_000L);
            assertThat(global.tranche90Plus()).isEqualTo(4_000_000L);
            assertThat(global.totalEncours()).isEqualTo(10_000_000L);
            assertThat(global.nbFactures()).isEqualTo(42L);
            assertThat(global.nbEnRetard()).isEqualTo(17L);
        }

        @Test
        @DisplayName("une colonne non renseignée vaut zéro")
        void colonneAbsente() {
            when(repository.findAgingGlobal()).thenReturn(new Object[] { null, null, null, null, null, null, null });

            VieillissementGlobalDTO global = service.getAgingGlobal();

            assertThat(global.totalEncours()).isZero();
            assertThat(global.nbFactures()).isZero();
        }

        @Test
        @DisplayName("une officine à jour affiche des tranches nulles")
        void officineAJour() {
            assertThat(service.getAgingGlobal().totalEncours()).isZero();
        }
    }

    // ===== par organisme =====

    @Nested
    @DisplayName("Détail par organisme")
    class DetailParOrganisme {

        @Test
        @DisplayName("chaque organisme porte ses tranches, son encours et son délai")
        void contenuDeLaLigne() {
            when(repository.countAgingByOrganisme()).thenReturn(1L);
            when(repository.findAgingByOrganisme(0, 20)).thenReturn(
                List.<Object[]>of(organisme("CNAM", 45, 10_000_000L, 6_000_000L, 2_000_000L, 1_500_000L, 500_000L, 30L, 8L, 40))
            );

            DsoOrganismeDTO ligne = service.getDsoByOrganisme(PageRequest.of(0, 20)).getContent().getFirst();

            assertThat(ligne.organisme()).isEqualTo("CNAM");
            assertThat(ligne.delaiReglement()).isEqualTo(45);
            assertThat(ligne.encours()).isEqualTo(10_000_000L);
            assertThat(ligne.tranche0_30()).isEqualTo(6_000_000L);
            assertThat(ligne.tranche31_60()).isEqualTo(2_000_000L);
            assertThat(ligne.tranche61_90()).isEqualTo(1_500_000L);
            assertThat(ligne.tranche90Plus()).isEqualTo(500_000L);
            assertThat(ligne.nbFactures()).isEqualTo(30L);
            assertThat(ligne.nbEnRetard()).isEqualTo(8L);
            assertThat(ligne.dsoJours()).isEqualTo(40);
        }

        @Test
        @DisplayName("la pagination demandée est transmise en décalage et en taille")
        void paginationTransmise() {
            service.getDsoByOrganisme(PageRequest.of(2, 15));

            verify(repository).findAgingByOrganisme(30, 15);
        }

        @Test
        @DisplayName("le nombre total d'organismes accompagne la page")
        void totalAccompagneLaPage() {
            when(repository.countAgingByOrganisme()).thenReturn(37L);

            Page<DsoOrganismeDTO> page = service.getDsoByOrganisme(PageRequest.of(0, 20));

            assertThat(page.getTotalElements()).isEqualTo(37L);
            assertThat(page.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("aucun organisme rend une page vide")
        void aucunOrganisme() {
            assertThat(service.getDsoByOrganisme(PageRequest.of(0, 20)).getContent()).isEmpty();
        }
    }

    // ===== fiabilité =====

    @Nested
    @DisplayName("Verdict de fiabilité")
    class VerdictDeFiabilite {

        /** Plus du quart de l'encours au-delà du trimestre : le payeur ne règle plus. */
        @Test
        @DisplayName("un quart de l'encours au-delà du trimestre classe en risque")
        void tropDeVieuxImpayes() {
            assertThat(fiabilite(10_000_000L, 2_500_001L, 40, 45)).isEqualTo("RISQUE");
        }

        @Test
        @DisplayName("un délai moyen de règlement au-delà du trimestre classe en risque")
        void delaiMoyenTropLong() {
            assertThat(fiabilite(10_000_000L, 0L, 91, 45)).isEqualTo("RISQUE");
        }

        @Test
        @DisplayName("un dixième de l'encours au-delà du trimestre appelle la surveillance")
        void surveillanceParLAnciennete() {
            assertThat(fiabilite(10_000_000L, 1_000_001L, 40, 45)).isEqualTo("SURVEILLER");
        }

        /** Trente jours de retard sur le délai convenu : le payeur glisse. */
        @Test
        @DisplayName("un mois de retard sur le délai convenu appelle la surveillance")
        void surveillanceParLeRetard() {
            assertThat(fiabilite(10_000_000L, 0L, 76, 45)).isEqualTo("SURVEILLER");
        }

        /**
         * Le délai convenu ne descend jamais sous trente jours pour ce calcul : un organisme qui
         * paierait sous huitaine ne doit pas basculer en surveillance au moindre jour de décalage.
         */
        @Test
        @DisplayName("un délai convenu très court est relevé à trente jours pour juger du retard")
        void delaiPlancher() {
            assertThat(fiabilite(10_000_000L, 0L, 60, 7)).isEqualTo("BON");
            assertThat(fiabilite(10_000_000L, 0L, 61, 7)).isEqualTo("SURVEILLER");
        }

        @Test
        @DisplayName("un payeur régulier est jugé bon")
        void payeurRegulier() {
            assertThat(fiabilite(10_000_000L, 500_000L, 30, 45)).isEqualTo("BON");
        }

        /** Sans encours il n'y a rien à juger : le verdict est favorable par défaut. */
        @Test
        @DisplayName("un organisme sans encours est jugé bon")
        void sansEncours() {
            assertThat(fiabilite(0L, 0L, 200, 45)).isEqualTo("BON");
        }

        private String fiabilite(long encours, long tranche90Plus, int dso, int delai) {
            when(repository.countAgingByOrganisme()).thenReturn(1L);
            when(repository.findAgingByOrganisme(0, 20)).thenReturn(
                List.<Object[]>of(organisme("CNAM", delai, encours, 0L, 0L, 0L, tranche90Plus, 1L, 0L, dso))
            );

            return service.getDsoByOrganisme(PageRequest.of(0, 20)).getContent().getFirst().fiabilite();
        }
    }

    // ===== encours mensuel =====

    @Nested
    @DisplayName("Évolution de l'encours")
    class EvolutionDeLEncours {

        @Test
        @DisplayName("chaque mois porte son libellé, le facturé et ce qui reste dû")
        void contenuDeLaCourbe() {
            when(repository.findEncoursMensuelEvolution()).thenReturn(
                List.<Object[]>of(new Object[] { 2026, 1, 8_000_000L, 3_000_000L }, new Object[] { 2026, 2, 9_000_000L, 5_000_000L })
            );

            EncoursMensuelDTO evolution = service.getEncoursMensuelEvolution();

            assertThat(evolution.labels()).containsExactly(libelle(2026, 1), libelle(2026, 2));
            assertThat(evolution.montantFacture()).containsExactly(8_000_000L, 9_000_000L);
            assertThat(evolution.encoursRestant()).containsExactly(3_000_000L, 5_000_000L);
        }

        @Test
        @DisplayName("un mois sans facture vaut zéro et ne disparaît pas de la courbe")
        void moisSansFacture() {
            when(repository.findEncoursMensuelEvolution()).thenReturn(
                List.<Object[]>of(new Object[] { 2026, 1, null, null }, new Object[] { 2026, 2, 9_000_000L, 5_000_000L })
            );

            EncoursMensuelDTO evolution = service.getEncoursMensuelEvolution();

            assertThat(evolution.montantFacture()).containsExactly(0L, 9_000_000L);
            assertThat(evolution.encoursRestant()).containsExactly(0L, 5_000_000L);
        }

        @Test
        @DisplayName("les trois séries ont toujours la même longueur")
        void seriesDeMemeLongueur() {
            when(repository.findEncoursMensuelEvolution()).thenReturn(
                List.<Object[]>of(new Object[] { 2025, 12, 1L, 1L }, new Object[] { 2026, 1, 2L, 2L })
            );

            EncoursMensuelDTO evolution = service.getEncoursMensuelEvolution();

            assertThat(evolution.montantFacture()).hasSameSizeAs(evolution.labels());
            assertThat(evolution.encoursRestant()).hasSameSizeAs(evolution.labels());
        }

        @Test
        @DisplayName("aucune facture rend une courbe vide")
        void aucuneFacture() {
            assertThat(service.getEncoursMensuelEvolution().labels()).isEmpty();
        }
    }

    // ===== utilitaires =====

    private static Object[] organisme(
        String nom,
        int delai,
        long encours,
        long t030,
        long t3160,
        long t6190,
        long t90p,
        long nbFactures,
        long nbRetard,
        int dso
    ) {
        return new Object[] { nom, delai, encours, t030, t3160, t6190, t90p, nbFactures, nbRetard, dso };
    }

    private static String libelle(int annee, int mois) {
        return YearMonth.of(annee, mois).atDay(1).format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH));
    }
}
