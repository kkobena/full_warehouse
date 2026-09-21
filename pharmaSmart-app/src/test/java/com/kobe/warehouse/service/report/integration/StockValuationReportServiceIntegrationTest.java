package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.MvStockValuationByRayonView;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.StockValuationView;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.service.dto.report.StockValuationSummaryDTO;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * La valorisation du stock répond à une question comptable : combien vaut ce qu'il y a en rayon, à
 * l'achat et à la vente, et quelle marge dort entre les deux. C'est le chiffre qu'on reporte à
 * l'inventaire et qu'un expert-comptable relit.
 *
 * <p>Il se calcule dans deux vues matérialisées jumelles — l'une par produit, l'autre par rayon —
 * que le service choisit selon les filtres reçus. Deux choses valent d'être tenues. Le
 * <b>cloisonnement par magasin</b> d'abord : la valorisation se lit toujours pour le magasin de
 * l'utilisateur connecté, jamais pour l'ensemble, sinon un réseau d'officines additionnerait des
 * stocks qui ne sont pas les siens. Et le <b>taux de marge</b> ensuite, dont la division a déjà été
 * cassée deux fois — {@code prix_uni} et {@code prix_achat} sont entiers, et une division entière
 * y renvoyait zéro sur chaque ligne, à côté d'une marge potentielle de plusieurs millions. V2.0.6
 * l'a corrigée ; le test ci-dessous l'y maintient.
 *
 * <p>Le périmètre est réduit à ce qui a du stock : un produit à zéro n'a rien à valoriser, et son
 * absence n'est pas un oubli.
 */
@DisplayName("StockValuationReportService — valorisation du stock sur mv_stock_valuation")
class StockValuationReportServiceIntegrationTest extends AbstractReportIntegrationTest {

    private static final String VUE = "mv_stock_valuation";
    private static final String VUE_RAYON = "mv_stock_valuation_by_rayon";

    // ===== valorisation par produit =====

    @Nested
    @DisplayName("Valorisation par produit")
    class ParProduit {

        @Test
        @DisplayName("valorise le stock à l'achat et à la vente, et la marge qui dort entre les deux")
        void valeursDAchatEtDeVente() {
            produitEnStock("DOLIPRANE", 10); // achat 6 000, vente 10 000
            rafraichir(VUE);

            StockValuationView ligne = services.stockValuationReportService.getStockValuation(null, null).getFirst();

            assertThat(ligne.getStockQuantity()).isEqualTo(10);
            assertThat(ligne.getPurchasePrice()).isEqualTo(6_000);
            assertThat(ligne.getSalesPrice()).isEqualTo(10_000);
            assertThat(ligne.getTotalPurchaseValue()).isEqualTo(60_000L);
            assertThat(ligne.getTotalSalesValue()).isEqualTo(100_000L);
            assertThat(ligne.getPotentialMargin()).isEqualTo(40_000L);
        }

        /**
         * Le taux de marge a été cassé deux fois par une division entière — V1.1.7 l'avait corrigé,
         * V1.7.7 l'a réintroduit, V2.0.6 l'a recorrigé. Quarante pour cent, et non zéro : c'est
         * exactement ce que ce test empêche de revenir.
         */
        @Test
        @DisplayName("le taux de marge est un pourcentage réel, et non zéro par division entière")
        void tauxDeMarge() {
            produitEnStock("DOLIPRANE", 10);
            rafraichir(VUE);

            StockValuationView ligne = services.stockValuationReportService.getStockValuation(null, null).getFirst();

            assertThat(ligne.getMarginPercentage()).isEqualByComparingTo("40.00");
        }

        @Test
        @DisplayName("un produit sans stock n'a rien à valoriser")
        void produitSansStockAbsent() {
            produitEnStock("EN RUPTURE", 0);
            rafraichir(VUE);

            assertThat(services.stockValuationReportService.getStockValuation(null, null)).isEmpty();
        }

        @Test
        @DisplayName("les produits au détail comptent aussi : l'inventaire ne les oublie pas")
        void produitDetailInclus() {
            Produit detail = produit(unique("AU DETAIL"), TypeProduit.DETAIL, 10_000, 5);
            referencement(detail, fournisseur("LABOREX " + unique("")));
            stock(detail, 5);
            rafraichir(VUE);

            assertThat(services.stockValuationReportService.getStockValuation(null, null)).hasSize(1);
        }

        @Test
        @DisplayName("les lignes sont classées par valeur de vente décroissante")
        void ordreParValeur() {
            produitEnStock("PETIT", 1);
            produitEnStock("GROS", 100);
            produitEnStock("MOYEN", 10);
            rafraichir(VUE);

            assertThat(services.stockValuationReportService.getStockValuation(null, null))
                .extracting(StockValuationView::getLibelle)
                .containsExactly("GROS", "MOYEN", "PETIT");
        }

        @Test
        @DisplayName("la pagination borne les lignes et rapporte le total")
        void pagination() {
            produitEnStock("PETIT", 1);
            produitEnStock("GROS", 100);
            produitEnStock("MOYEN", 10);
            rafraichir(VUE);

            Page<StockValuationView> page = services.stockValuationReportService.getStockValuationPaginated(null, null, PageRequest.of(0, 2));

            assertThat(page.getContent()).extracting(StockValuationView::getLibelle).containsExactly("GROS", "MOYEN");
            assertThat(page.getTotalElements()).isEqualTo(3);
        }

        /** Le filtre par famille sert à valoriser un rayon comptable — parapharmacie contre médicament. */
        @Test
        @DisplayName("le filtre par famille retient les produits de cette famille")
        void filtreParFamille() {
            produitEnStock("DOLIPRANE", 10);
            rafraichir(VUE);

            int familleId = premiereFamille().getId();

            assertThat(services.stockValuationReportService.getStockValuation(familleId, null)).hasSize(1);
            assertThat(services.stockValuationReportService.getStockValuation(999_999, null)).isEmpty();
        }

        /** Zéro veut dire « pas de filtre » — c'est la valeur que l'écran envoie quand rien n'est choisi. */
        @Test
        @DisplayName("une famille à zéro vaut absence de filtre")
        void familleZeroVautTout() {
            produitEnStock("DOLIPRANE", 10);
            rafraichir(VUE);

            assertThat(services.stockValuationReportService.getStockValuation(0, 0)).hasSize(1);
        }
    }

    // ===== valorisation par rayon =====

    @Nested
    @DisplayName("Valorisation par rayon")
    class ParRayon {

        @Test
        @DisplayName("le filtre par rayon bascule sur la vue détaillée et ne rend que ce rayon")
        void filtreParRayon() {
            Produit auComptoir = produitEnStock("AU COMPTOIR", 10);
            Rayon comptoir = rangeAuRayon(auComptoir, "COMPTOIR");

            Produit enVitrine = produitEnStock("EN VITRINE", 10);
            rangeAuRayon(enVitrine, "VITRINE");
            rafraichir(VUE_RAYON);

            List<StockValuationView> lignes = services.stockValuationReportService.getStockValuation(null, comptoir.getId());

            assertThat(lignes).extracting(StockValuationView::getLibelle).containsExactly("AU COMPTOIR");
            assertThat(lignes.getFirst()).isInstanceOf(MvStockValuationByRayonView.class);
        }

        @Test
        @DisplayName("le rayon et la famille se combinent")
        void rayonEtFamilleSeCombinent() {
            Produit produit = produitEnStock("AU COMPTOIR", 10);
            Rayon comptoir = rangeAuRayon(produit, "COMPTOIR");
            rafraichir(VUE_RAYON);

            int familleId = premiereFamille().getId();

            assertThat(services.stockValuationReportService.getStockValuation(familleId, comptoir.getId())).hasSize(1);
            assertThat(services.stockValuationReportService.getStockValuation(999_999, comptoir.getId())).isEmpty();
        }

        @Test
        @DisplayName("la pagination par rayon borne les lignes")
        void paginationParRayon() {
            Produit petit = produitEnStock("PETIT", 1);
            Rayon comptoir = rangeAuRayon(petit, "COMPTOIR");

            Produit gros = produitEnStock("GROS", 100);
            rattacheAuRayon(gros, comptoir);
            rafraichir(VUE_RAYON);

            Page<StockValuationView> page = services.stockValuationReportService.getStockValuationPaginated(
                null,
                comptoir.getId(),
                PageRequest.of(0, 1)
            );

            assertThat(page.getContent()).extracting(StockValuationView::getLibelle).containsExactly("GROS");
            assertThat(page.getTotalElements()).isEqualTo(2);
        }
    }

    // ===== synthèse =====

    @Nested
    @DisplayName("Synthèse")
    class Synthese {

        @Test
        @DisplayName("la synthèse totalise valeurs, marge, produits et quantités")
        void totaux() {
            produitEnStock("DOLIPRANE", 10);
            produitEnStock("EFFERALGAN", 5);
            rafraichir(VUE);

            StockValuationSummaryDTO synthese = services.stockValuationReportService.getStockValuationSummary();

            assertThat(synthese.totalProducts()).isEqualTo(2);
            assertThat(synthese.totalQuantity()).isEqualTo(15);
            assertThat(synthese.totalPurchaseValue()).isEqualTo(90_000L);
            assertThat(synthese.totalSalesValue()).isEqualTo(150_000L);
            assertThat(synthese.totalPotentialMargin()).isEqualTo(60_000L);
            assertThat(synthese.averageMarginPercentage()).isEqualByComparingTo("40.00");
        }

        @Test
        @DisplayName("la synthèse filtrée suit les mêmes filtres que la liste")
        void syntheseFiltree() {
            produitEnStock("DOLIPRANE", 10);
            rafraichir(VUE);

            int familleId = premiereFamille().getId();

            assertThat(services.stockValuationReportService.getStockValuationSummary(familleId, 0).totalProducts()).isEqualTo(1);
            assertThat(services.stockValuationReportService.getStockValuationSummary(999_999, 0).totalProducts()).isZero();
        }

        @Test
        @DisplayName("la synthèse par rayon lit la vue détaillée")
        void syntheseParRayon() {
            Produit produit = produitEnStock("AU COMPTOIR", 10);
            Rayon comptoir = rangeAuRayon(produit, "COMPTOIR");
            produitEnStock("AILLEURS", 50);
            rafraichir(VUE_RAYON);

            StockValuationSummaryDTO synthese = services.stockValuationReportService.getStockValuationSummary(null, comptoir.getId());

            assertThat(synthese.totalProducts()).isEqualTo(1);
            assertThat(synthese.totalQuantity()).isEqualTo(10);
        }

        /**
         * Une vue vide ne rend pas « aucune ligne » mais une ligne de {@code NULL} : les sommes d'un
         * ensemble vide sont nulles en SQL. Sans garde, la synthèse remonterait des
         * {@code NullPointerException} plutôt que des zéros.
         */
        @Test
        @DisplayName("base vierge : des zéros, et non des nuls")
        void baseVierge() {
            rafraichir(VUE);

            StockValuationSummaryDTO synthese = services.stockValuationReportService.getStockValuationSummary();

            assertThat(synthese.totalProducts()).isZero();
            assertThat(synthese.totalQuantity()).isZero();
            assertThat(synthese.totalPurchaseValue()).isZero();
            assertThat(synthese.totalSalesValue()).isZero();
            assertThat(synthese.totalPotentialMargin()).isZero();
            assertThat(synthese.averageMarginPercentage()).isEqualByComparingTo("0");
        }
    }

    // ===== fabriques locales =====

    /** Un produit référencé et stocké : sans référencement principal, la vue n'a pas de prix à appliquer. */
    private Produit produitEnStock(String libelle, int quantite) {
        Produit produit = produit(libelle, TypeProduit.PACKAGE, 10_000, 5);
        referencement(produit, fournisseur("LABOREX " + unique("")));
        stock(produit, quantite);
        return produit;
    }
}
