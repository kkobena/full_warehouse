package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.ClassePareto;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.service.dto.report.ABCParetoDTO;
import com.kobe.warehouse.service.dto.report.ABCParetoSummaryDTO;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * L'analyse ABC classe les produits par leur part du chiffre d'affaires cumulé des douze derniers
 * mois : les premiers 60 % en {@code A_PLUS}, jusqu'à 80 % en {@code A}, puis {@code B}, {@code C},
 * et {@code D} pour la queue et pour tout ce qui ne s'est pas vendu. C'est cette classification qui
 * décide de l'attention qu'un produit reçoit — un {@code A_PLUS} ne doit jamais tomber en rupture,
 * un {@code D} ne mérite pas qu'on immobilise de la trésorerie dessus.
 *
 * <p>Le calcul vit dans {@code v_abc_pareto_analysis}, donc dans les migrations ; le service se
 * contente d'y lire quinze colonnes qu'il nomme à la main et de traduire la classe en énumération.
 * Trois choses peuvent alors se décaler sans bruit. Le <b>périmètre de la vue</b> : elle écarte les
 * produits {@code DETAIL} et ne compte que les ventes clôturées, non annulées et rangées en chiffre
 * d'affaires — un jeu d'essai qui manque l'une de ces conditions rend une vue vide. Le <b>rang</b>,
 * qui ordonne toutes les autres méthodes du service, pagination comprise. Et la <b>synthèse</b>,
 * dont les pourcentages se divisent par un chiffre d'affaires global qui peut être nul.
 */
@DisplayName("ABCParetoReportService — analyse ABC sur v_abc_pareto_analysis")
class ABCParetoReportServiceIntegrationTest extends AbstractReportIntegrationTest {

    // ===== périmètre de la vue =====

    @Nested
    @DisplayName("Périmètre")
    class Perimetre {

        /**
         * Un produit sans vente n'a pas disparu de l'analyse : il y figure en {@code D}. C'est ce
         * qui permet à l'acheteur de voir ce qui dort en rayon, et non seulement ce qui tourne.
         */
        @Test
        @DisplayName("un produit jamais vendu est classé D plutôt qu'omis")
        void produitSansVenteClasseD() {
            produitReference("DOLIPRANE", 10_000);
            viderLeCache();

            List<ABCParetoDTO> analyse = services.abcParetoReportService.getAllABCParetoAnalysis();

            assertThat(analyse).hasSize(1);
            assertThat(analyse.getFirst().classePareto()).isEqualTo(ClassePareto.D);
            assertThat(analyse.getFirst().caTotal()).isZero();
        }

        /** Les produits au détail relèvent d'une autre logique de gestion : la vue les écarte. */
        @Test
        @DisplayName("un produit au détail est hors de l'analyse")
        void produitDetailExclu() {
            Produit detail = produit(unique("AU DETAIL"), TypeProduit.DETAIL, 10_000, 5);
            referencement(detail, fournisseur("LABOREX " + unique("")));
            vendu(detail, 10, LocalDate.now());
            viderLeCache();

            assertThat(services.abcParetoReportService.getAllABCParetoAnalysis()).isEmpty();
        }

        /**
         * Les quatre conditions de la vue vivent dans la clause {@code ON} d'un {@code LEFT JOIN},
         * où elles ne suppriment aucune ligne — elles ne font qu'annuler {@code s}. Sans garde sur
         * les agrégats, la ligne de vente continuait donc de compter, et l'analyse « douze derniers
         * mois » recouvrait tout l'historique. C'est ce que V2.0.8 a corrigé ; ces trois tests
         * empêchent la régression.
         */
        @Test
        @DisplayName("une vente d'il y a plus de douze mois ne pèse plus dans le classement")
        void venteHorsFenetreIgnoree() {
            Produit produit = produitReference("DOLIPRANE", 10_000);
            vendu(produit, 50, LocalDate.now().minusMonths(13));
            viderLeCache();

            ABCParetoDTO resultat = services.abcParetoReportService.getAllABCParetoAnalysis().getFirst();

            assertThat(resultat.caTotal()).isZero();
            assertThat(resultat.qteVendue()).isZero();
            assertThat(resultat.nbVentes()).isZero();
            assertThat(resultat.classePareto()).isEqualTo(ClassePareto.D);
        }

        @Test
        @DisplayName("une vente annulée ne pèse pas dans le classement")
        void venteAnnuleeIgnoree() {
            Produit produit = produitReference("DOLIPRANE", 10_000);
            vendu(produit, 50, LocalDate.now(), true, CategorieChiffreAffaire.CA);
            viderLeCache();

            ABCParetoDTO resultat = services.abcParetoReportService.getAllABCParetoAnalysis().getFirst();

            assertThat(resultat.caTotal()).isZero();
            assertThat(resultat.classePareto()).isEqualTo(ClassePareto.D);
        }

        @Test
        @DisplayName("une vente hors chiffre d'affaires ne pèse pas dans le classement")
        void venteHorsChiffreDAffairesIgnoree() {
            Produit produit = produitReference("DOLIPRANE", 10_000);
            vendu(produit, 50, LocalDate.now(), false, CategorieChiffreAffaire.CA_DEPOT);
            viderLeCache();

            ABCParetoDTO resultat = services.abcParetoReportService.getAllABCParetoAnalysis().getFirst();

            assertThat(resultat.caTotal()).isZero();
            assertThat(resultat.classePareto()).isEqualTo(ClassePareto.D);
        }

        @Test
        @DisplayName("base vierge : aucune ligne, et une synthèse à zéro plutôt qu'une erreur")
        void baseVierge() {
            assertThat(services.abcParetoReportService.getAllABCParetoAnalysis()).isEmpty();
            assertThat(services.abcParetoReportService.getABCParetoCount()).isZero();

            ABCParetoSummaryDTO synthese = services.abcParetoReportService.getABCParetoSummary();

            assertThat(synthese.totalProduits()).isZero();
            assertThat(synthese.caGlobal()).isZero();
            assertThat(synthese.pctCaClasseA()).isZero();
        }
    }

    // ===== classement =====

    @Nested
    @DisplayName("Classement")
    class Classement {

        /**
         * Le rang est l'ordre du chiffre d'affaires décroissant : c'est lui qui gouverne toutes les
         * autres lectures du service, et la pagination en dépend entièrement.
         */
        @Test
        @DisplayName("les produits sont rangés par chiffre d'affaires décroissant")
        void rangParChiffreDAffaires() {
            vendu(produitReference("PETIT", 1_000), 1, LocalDate.now());
            vendu(produitReference("GROS", 100_000), 10, LocalDate.now());
            vendu(produitReference("MOYEN", 10_000), 5, LocalDate.now());
            viderLeCache();

            List<ABCParetoDTO> analyse = services.abcParetoReportService.getAllABCParetoAnalysis();

            assertThat(analyse).extracting(ABCParetoDTO::libelle).containsExactly("GROS", "MOYEN", "PETIT");
            assertThat(analyse).extracting(ABCParetoDTO::rang).containsExactly(1, 2, 3);
        }

        /**
         * La classe se lit sur le chiffre d'affaires <b>cumulé</b> au moment où l'on atteint le
         * produit, contre les paliers 60 / 80 / 95 / 99. Le jeu d'essai tombe exactement sur ces
         * quatre bornes : c'est le seul endroit où la règle se distingue d'une autre, et c'est
         * cette classe qui décide ensuite de l'attention qu'un produit reçoit — un {@code A_PLUS}
         * ne doit jamais manquer, un {@code D} ne mérite pas qu'on immobilise de l'argent dessus.
         */
        @Test
        @DisplayName("les cinq classes se déduisent du chiffre d'affaires cumulé")
        void cinqClassesSelonLeCumul() {
            distributionPareto();
            viderLeCache();

            List<ABCParetoDTO> analyse = services.abcParetoReportService.getAllABCParetoAnalysis();

            assertThat(analyse)
                .extracting(ABCParetoDTO::libelle, ABCParetoDTO::classePareto)
                .containsExactly(
                    tuple("P60", ClassePareto.A_PLUS),
                    tuple("P20", ClassePareto.A),
                    tuple("P15", ClassePareto.B),
                    tuple("P04", ClassePareto.C),
                    tuple("P01", ClassePareto.D)
                );
        }

        @Test
        @DisplayName("la contribution de chaque produit rapporte son chiffre d'affaires au global")
        void contributionParProduit() {
            distributionPareto();
            viderLeCache();

            List<ABCParetoDTO> analyse = services.abcParetoReportService.getAllABCParetoAnalysis();

            assertThat(analyse.getFirst().contributionPct()).isEqualByComparingTo("60.00");
            assertThat(analyse.get(1).contributionPct()).isEqualByComparingTo("20.00");
        }

        @Test
        @DisplayName("le chiffre d'affaires cumulé croît et le pourcentage atteint cent")
        void cumulCoherent() {
            vendu(produitReference("GROS", 100_000), 10, LocalDate.now());
            vendu(produitReference("MOYEN", 10_000), 5, LocalDate.now());
            viderLeCache();

            List<ABCParetoDTO> analyse = services.abcParetoReportService.getAllABCParetoAnalysis();

            assertThat(analyse.getFirst().caCumule()).isEqualTo(1_000_000L);
            assertThat(analyse.get(1).caCumule()).isEqualTo(1_050_000L);
            assertThat(analyse.get(1).caCumulePct()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("les quantités et le nombre de ventes suivent le produit")
        void quantitesEtNombreDeVentes() {
            Produit produit = produitReference("DOLIPRANE", 10_000);
            vendu(produit, 3, LocalDate.now());
            vendu(produit, 7, LocalDate.now().minusDays(1));
            viderLeCache();

            ABCParetoDTO resultat = services.abcParetoReportService.getAllABCParetoAnalysis().getFirst();

            assertThat(resultat.qteVendue()).isEqualTo(10);
            assertThat(resultat.nbVentes()).isEqualTo(2);
            assertThat(resultat.caTotal()).isEqualTo(100_000);
        }

        @Test
        @DisplayName("le code CIP du référencement principal accompagne chaque ligne")
        void codeCipReporte() {
            produitReference("DOLIPRANE", 10_000);
            viderLeCache();

            assertThat(services.abcParetoReportService.getAllABCParetoAnalysis().getFirst().codeCip()).isNotBlank();
        }
    }

    // ===== lectures filtrées =====

    @Nested
    @DisplayName("Lectures filtrées")
    class LecturesFiltrees {

        @Test
        @DisplayName("le filtre par classe ne rend que les produits de cette classe")
        void filtreParClasse() {
            distributionPareto();
            viderLeCache();

            assertThat(services.abcParetoReportService.getABCParetoByClass(ClassePareto.A_PLUS))
                .extracting(ABCParetoDTO::libelle)
                .containsExactly("P60");
            assertThat(services.abcParetoReportService.getABCParetoByClass(ClassePareto.B))
                .extracting(ABCParetoDTO::libelle)
                .containsExactly("P15");
        }

        /** Les produits sans vente rejoignent la classe D, aux côtés de la queue du classement. */
        @Test
        @DisplayName("le comptage par classe s'accorde avec la liste")
        void comptageParClasse() {
            distributionPareto();
            produitReference("DORMANT A", 5_000);
            produitReference("DORMANT B", 5_000);
            viderLeCache();

            assertThat(services.abcParetoReportService.getABCParetoCountByClass(ClassePareto.D)).isEqualTo(3);
            assertThat(services.abcParetoReportService.getABCParetoCountByClass(ClassePareto.A_PLUS)).isEqualTo(1);
            assertThat(services.abcParetoReportService.getABCParetoCount()).isEqualTo(7);
        }

        @Test
        @DisplayName("les meilleurs contributeurs sont les premiers du classement")
        void meilleursContributeurs() {
            vendu(produitReference("GROS", 100_000), 10, LocalDate.now());
            vendu(produitReference("MOYEN", 10_000), 5, LocalDate.now());
            vendu(produitReference("PETIT", 1_000), 1, LocalDate.now());
            viderLeCache();

            assertThat(services.abcParetoReportService.getTopRevenueContributors(2))
                .extracting(ABCParetoDTO::libelle)
                .containsExactly("GROS", "MOYEN");
        }

        @Test
        @DisplayName("la pagination découpe le classement sans le réordonner")
        void pagination() {
            vendu(produitReference("GROS", 100_000), 10, LocalDate.now());
            vendu(produitReference("MOYEN", 10_000), 5, LocalDate.now());
            vendu(produitReference("PETIT", 1_000), 1, LocalDate.now());
            viderLeCache();

            assertThat(services.abcParetoReportService.getABCParetoPaginated(0, 2))
                .extracting(ABCParetoDTO::libelle)
                .containsExactly("GROS", "MOYEN");
            assertThat(services.abcParetoReportService.getABCParetoPaginated(1, 2))
                .extracting(ABCParetoDTO::libelle)
                .containsExactly("PETIT");
        }

        @Test
        @DisplayName("le filtre par famille suit le rattachement du produit")
        void filtreParFamille() {
            produitReference("DOLIPRANE", 10_000);
            viderLeCache();

            String famille = premiereFamille().getLibelle();

            assertThat(services.abcParetoReportService.getABCParetoByCategory(famille))
                .extracting(ABCParetoDTO::libelle)
                .containsExactly("DOLIPRANE");
            assertThat(services.abcParetoReportService.getABCParetoByCategory("FAMILLE INEXISTANTE")).isEmpty();
        }
    }

    // ===== synthèse =====

    @Nested
    @DisplayName("Synthèse")
    class Synthese {

        @Test
        @DisplayName("la synthèse totalise les produits et le chiffre d'affaires de toutes les classes")
        void totauxParClasse() {
            distributionPareto();
            produitReference("DORMANT", 5_000);
            viderLeCache();

            ABCParetoSummaryDTO synthese = services.abcParetoReportService.getABCParetoSummary();

            assertThat(synthese.totalProduits()).isEqualTo(6);
            assertThat(synthese.caGlobal()).isEqualTo(1_000_000L);
            assertThat(synthese.nbProduitsAPlus()).isEqualTo(1);
            assertThat(synthese.caClasseAPlus()).isEqualTo(600_000L);
            assertThat(synthese.nbProduitsD()).isEqualTo(2);
            assertThat(synthese.caClasseD()).isEqualTo(10_000L);
        }

        @Test
        @DisplayName("les pourcentages de chaque classe rapportent son chiffre d'affaires au global")
        void pourcentages() {
            distributionPareto();
            viderLeCache();

            ABCParetoSummaryDTO synthese = services.abcParetoReportService.getABCParetoSummary();

            assertThat(synthese.pctCaClasseAPlus()).isEqualByComparingTo("60.00");
            assertThat(synthese.pctCaClasseA()).isEqualByComparingTo("20.00");
            assertThat(synthese.pctCaClasseB()).isEqualByComparingTo("15.00");
            assertThat(synthese.pctCaClasseC()).isEqualByComparingTo("4.00");
            assertThat(synthese.pctCaClasseD()).isEqualByComparingTo("1.00");
        }

        /**
         * Sans aucune vente, le chiffre d'affaires global est nul : la division qui calcule les
         * pourcentages doit s'en accommoder plutôt que de remonter une erreur jusqu'à l'écran.
         */
        @Test
        @DisplayName("sans vente, les pourcentages valent zéro et non une division par zéro")
        void aucuneVente() {
            produitReference("DORMANT", 5_000);
            viderLeCache();

            ABCParetoSummaryDTO synthese = services.abcParetoReportService.getABCParetoSummary();

            assertThat(synthese.totalProduits()).isEqualTo(1);
            assertThat(synthese.caGlobal()).isZero();
            assertThat(synthese.pctCaClasseD()).isZero();
            assertThat(synthese.pctCaClasseAPlus()).isZero();
        }
    }

    // ===== fabrique locale =====

    /**
     * Cinq produits portant 60, 20, 15, 4 et 1 % du chiffre d'affaires. Les cumuls tombent
     * exactement sur les quatre paliers de la vue — 60, 80, 95 et 99 — de sorte que chaque classe
     * est représentée par un produit et un seul.
     */
    private void distributionPareto() {
        vendu(produitReference("P60", 10_000), 60, LocalDate.now());
        vendu(produitReference("P20", 10_000), 20, LocalDate.now());
        vendu(produitReference("P15", 10_000), 15, LocalDate.now());
        vendu(produitReference("P04", 10_000), 4, LocalDate.now());
        vendu(produitReference("P01", 10_000), 1, LocalDate.now());
    }

    /** Un produit analysable et référencé : sans référencement, la vue n'a pas de code CIP à rendre. */
    private Produit produitReference(String libelle, int prixVente) {
        Produit produit = produitAnalysable(libelle, prixVente);
        Fournisseur fournisseur = fournisseur("LABOREX " + unique(""));
        referencement(produit, fournisseur);
        return produit;
    }
}
