package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.CategorieABC;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.service.dto.report.StockRotationDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * La rotation dit à quelle vitesse un stock se transforme en ventes. Deux chiffres la portent : la
 * <b>rotation annuelle</b> — combien de fois le stock actuel a été écoulé dans l'année — et la
 * <b>couverture</b>, le nombre de jours que ce stock tiendra au rythme observé. L'acheteur s'en
 * sert pour deux décisions opposées : commander ce qui va manquer, et cesser d'immobiliser de
 * l'argent sur ce qui dort.
 *
 * <p>Les deux se calculent dans {@code v_stock_rotation}, et les deux sont des divisions par des
 * quantités qui peuvent être nulles ou négatives. C'est là que tout se joue : un stock à zéro n'a
 * pas de rotation, un produit jamais vendu n'a pas de couverture, et un stock négatif — qui arrive,
 * après un inventaire — n'en a pas davantage. La vue rend {@code NULL} dans ces trois cas, et le
 * service doit les traduire en quelque chose d'affichable plutôt que de propager le vide.
 *
 * <p>La <b>classification</b> vient d'ailleurs : elle n'est plus calculée par la vue mais lue sur
 * {@code produit.classe_criticite}, puis repliée de cinq classes sur trois. Deux classes distinctes
 * se retrouvent donc sous la même étiquette, et les filtres par catégorie doivent en tenir compte.
 */
@DisplayName("StockRotationReportService — rotation du stock sur v_stock_rotation")
class StockRotationReportServiceIntegrationTest extends AbstractReportIntegrationTest {

    // ===== indicateurs =====

    @Nested
    @DisplayName("Indicateurs")
    class Indicateurs {

        @Test
        @DisplayName("la rotation annuelle rapporte les quantités vendues au stock restant")
        void rotationAnnuelle() {
            Produit produit = produitEnStock("DOLIPRANE", 10);
            vendu(produit, 40, LocalDate.now());
            viderLeCache();

            StockRotationDTO resultat = services.stockRotationReportService.getAllStockRotation().getFirst();

            assertThat(resultat.qtySoldLast12Months()).isEqualTo(40);
            assertThat(resultat.stockQuantity()).isEqualTo(10);
            assertThat(resultat.rotationRateAnnual()).isEqualByComparingTo("4.00");
        }

        /**
         * La couverture répond à « combien de jours puis-je tenir ». Quarante-huit unités vendues
         * dans l'année font quatre par mois ; douze en stock tiennent donc trois mois, soit
         * quatre-vingt-dix jours.
         */
        @Test
        @DisplayName("la couverture traduit le stock en jours de vente")
        void couvertureEnJours() {
            Produit produit = produitEnStock("DOLIPRANE", 12);
            vendu(produit, 48, LocalDate.now());
            viderLeCache();

            assertThat(services.stockRotationReportService.getAllStockRotation().getFirst().avgDaysInStock()).isEqualTo(90);
        }

        /**
         * Sans vente, la couverture est infinie et la vue rend {@code NULL}. Le service le traduit
         * en 999 — une valeur qui se trie en fin de liste et se lit comme « dort ».
         */
        @Test
        @DisplayName("un produit jamais vendu n'a pas de couverture : 999 par convention")
        void produitJamaisVendu() {
            produitEnStock("DORMANT", 50);
            viderLeCache();

            StockRotationDTO resultat = services.stockRotationReportService.getAllStockRotation().getFirst();

            assertThat(resultat.qtySoldLast12Months()).isZero();
            assertThat(resultat.avgDaysInStock()).isEqualTo(999);
            assertThat(resultat.rotationRateAnnual()).isEqualByComparingTo("0.00");
        }

        /** Un stock à zéro ne se divise pas : ni rotation ni couverture à en tirer. */
        @Test
        @DisplayName("un produit en rupture n'a ni rotation ni couverture")
        void produitEnRupture() {
            Produit produit = produitEnStock("EN RUPTURE", 0);
            vendu(produit, 100, LocalDate.now());
            viderLeCache();

            StockRotationDTO resultat = services.stockRotationReportService.getAllStockRotation().getFirst();

            assertThat(resultat.stockQuantity()).isZero();
            assertThat(resultat.rotationRateAnnual()).isEqualByComparingTo("0.00");
            assertThat(resultat.avgDaysInStock()).isEqualTo(999);
        }

        /**
         * Un stock négatif est le reliquat d'un écart d'inventaire. Le ramener à zéro évite qu'une
         * rotation négative vienne polluer le classement.
         */
        @Test
        @DisplayName("un stock négatif est ramené à zéro")
        void stockNegatifRameneAZero() {
            Produit produit = produitEnStock("INCOHERENT", -5);
            vendu(produit, 10, LocalDate.now());
            viderLeCache();

            StockRotationDTO resultat = services.stockRotationReportService.getAllStockRotation().getFirst();

            assertThat(resultat.stockQuantity()).isZero();
            assertThat(resultat.stockValue()).isZero();
        }

        @Test
        @DisplayName("la valeur du stock est valorisée au prix d'achat du fournisseur principal")
        void valeurDuStock() {
            produitEnStock("DOLIPRANE", 10); // prix de vente 10 000 → prix d'achat 6 000
            viderLeCache();

            StockRotationDTO resultat = services.stockRotationReportService.getAllStockRotation().getFirst();

            assertThat(resultat.unitCost()).isEqualTo(6_000);
            assertThat(resultat.stockValue()).isEqualTo(60_000L);
        }

        /** Le mois écoulé se lit à part : c'est lui qui montre un décrochage récent. */
        @Test
        @DisplayName("les ventes du mois se comptent séparément de celles de l'année")
        void ventesDuMoisAPart() {
            Produit produit = produitEnStock("DOLIPRANE", 10);
            vendu(produit, 3, LocalDate.now());
            vendu(produit, 7, LocalDate.now().minusDays(60));
            viderLeCache();

            StockRotationDTO resultat = services.stockRotationReportService.getAllStockRotation().getFirst();

            assertThat(resultat.qtySoldLast30Days()).isEqualTo(3);
            assertThat(resultat.nbSalesLast30Days()).isEqualTo(1);
            assertThat(resultat.qtySoldLast12Months()).isEqualTo(10);
            assertThat(resultat.caLast30Days()).isEqualTo(30_000);
            assertThat(resultat.caLast12Months()).isEqualTo(100_000);
        }
    }

    // ===== périmètre =====

    @Nested
    @DisplayName("Périmètre")
    class Perimetre {

        @Test
        @DisplayName("un produit au détail est hors du rapport")
        void produitDetailExclu() {
            Produit detail = produit(unique("AU DETAIL"), TypeProduit.DETAIL, 10_000, 5);
            referencement(detail, fournisseur("LABOREX " + unique("")));
            stock(detail, 10);
            viderLeCache();

            assertThat(services.stockRotationReportService.getAllStockRotation()).isEmpty();
        }

        @Test
        @DisplayName("une vente annulée ne compte pas dans la rotation")
        void venteAnnuleeIgnoree() {
            Produit produit = produitEnStock("DOLIPRANE", 10);
            vendu(produit, 40, LocalDate.now(), true, CategorieChiffreAffaire.CA);
            viderLeCache();

            assertThat(services.stockRotationReportService.getAllStockRotation().getFirst().qtySoldLast12Months()).isZero();
        }

        @Test
        @DisplayName("une vente d'il y a plus de douze mois ne compte plus")
        void venteHorsFenetreIgnoree() {
            Produit produit = produitEnStock("DOLIPRANE", 10);
            vendu(produit, 40, LocalDate.now().minusMonths(13));
            viderLeCache();

            assertThat(services.stockRotationReportService.getAllStockRotation().getFirst().qtySoldLast12Months()).isZero();
        }

        @Test
        @DisplayName("base vierge : aucune ligne et des compteurs à zéro")
        void baseVierge() {
            assertThat(services.stockRotationReportService.getAllStockRotation()).isEmpty();
            assertThat(services.stockRotationReportService.getStockRotationCount()).isZero();

            Map<CategorieABC, Long> compteurs = services.stockRotationReportService.getStockRotationCountByABCClassification();

            assertThat(compteurs)
                .containsEntry(CategorieABC.A, 0L)
                .containsEntry(CategorieABC.B, 0L)
                .containsEntry(CategorieABC.C, 0L);
        }
    }

    // ===== classement et filtres =====

    @Nested
    @DisplayName("Classement et filtres")
    class ClassementEtFiltres {

        @Test
        @DisplayName("les produits sont rangés par rotation décroissante")
        void ordreParRotation() {
            vendu(produitEnStock("RAPIDE", 10), 100, LocalDate.now());
            vendu(produitEnStock("LENT", 10), 10, LocalDate.now());
            viderLeCache();

            assertThat(services.stockRotationReportService.getAllStockRotation())
                .extracting(StockRotationDTO::libelle)
                .containsExactly("RAPIDE", "LENT");
        }

        /**
         * Cinq classes de criticité se replient sur trois catégories : {@code A_PLUS} et {@code A}
         * d'un côté, {@code C} et {@code D} de l'autre. Un filtre sur A doit donc ramener les deux
         * premières, sans quoi les produits vitaux disparaîtraient du rapport.
         */
        @Test
        @DisplayName("le filtre par catégorie replie les cinq classes de criticité sur trois")
        void filtreParCategorie() {
            classe(produitEnStock("VITAL", 10), ClasseCriticite.A_PLUS);
            classe(produitEnStock("FORTE", 10), ClasseCriticite.A);
            classe(produitEnStock("MOYENNE", 10), ClasseCriticite.B);
            classe(produitEnStock("FAIBLE", 10), ClasseCriticite.C);
            classe(produitEnStock("OBSOLETE", 10), ClasseCriticite.D);
            viderLeCache();

            assertThat(services.stockRotationReportService.getStockRotationByABCClassification(CategorieABC.A))
                .extracting(StockRotationDTO::libelle)
                .containsExactlyInAnyOrder("VITAL", "FORTE");
            assertThat(services.stockRotationReportService.getStockRotationByABCClassification(CategorieABC.B))
                .extracting(StockRotationDTO::libelle)
                .containsExactly("MOYENNE");
            assertThat(services.stockRotationReportService.getStockRotationByABCClassification(CategorieABC.C))
                .extracting(StockRotationDTO::libelle)
                .containsExactlyInAnyOrder("FAIBLE", "OBSOLETE");
        }

        @Test
        @DisplayName("la catégorie affichée suit le même repliement")
        void categorieAffichee() {
            classe(produitEnStock("VITAL", 10), ClasseCriticite.A_PLUS);
            classe(produitEnStock("OBSOLETE", 10), ClasseCriticite.D);
            viderLeCache();

            assertThat(services.stockRotationReportService.getAllStockRotation())
                .extracting(StockRotationDTO::libelle, StockRotationDTO::categorieABC)
                .containsExactlyInAnyOrder(tuple("VITAL", CategorieABC.A), tuple("OBSOLETE", CategorieABC.C));
        }

        @Test
        @DisplayName("les compteurs par catégorie s'accordent avec les filtres")
        void compteursParCategorie() {
            classe(produitEnStock("VITAL", 10), ClasseCriticite.A_PLUS);
            classe(produitEnStock("FORTE", 10), ClasseCriticite.A);
            classe(produitEnStock("OBSOLETE", 10), ClasseCriticite.D);
            viderLeCache();

            Map<CategorieABC, Long> compteurs = services.stockRotationReportService.getStockRotationCountByABCClassification();

            assertThat(compteurs)
                .containsEntry(CategorieABC.A, 2L)
                .containsEntry(CategorieABC.B, 0L)
                .containsEntry(CategorieABC.C, 1L);
            assertThat(services.stockRotationReportService.getStockRotationCountByABC(CategorieABC.A)).isEqualTo(2);
        }

        /** Le dormant se repère sur la valeur immobilisée, pas sur la rotation : c'est l'argent qui dort. */
        @Test
        @DisplayName("les produits peu actifs sont classés par valeur de stock décroissante")
        void produitsPeuActifs() {
            classe(produitEnStock("PETIT DORMANT", 1), ClasseCriticite.C);
            classe(produitEnStock("GROS DORMANT", 100), ClasseCriticite.D);
            classe(produitEnStock("ACTIF", 50), ClasseCriticite.A);
            viderLeCache();

            assertThat(services.stockRotationReportService.getSlowMovingProducts())
                .extracting(StockRotationDTO::libelle)
                .containsExactly("GROS DORMANT", "PETIT DORMANT");
        }

        @Test
        @DisplayName("le filtre par famille suit le rattachement du produit")
        void filtreParFamille() {
            produitEnStock("DOLIPRANE", 10);
            viderLeCache();

            assertThat(services.stockRotationReportService.getStockRotationByCategory(premiereFamille().getLibelle()))
                .extracting(StockRotationDTO::libelle)
                .containsExactly("DOLIPRANE");
            assertThat(services.stockRotationReportService.getStockRotationByCategory("FAMILLE INEXISTANTE")).isEmpty();
        }

        @Test
        @DisplayName("la pagination découpe le classement sans le réordonner")
        void pagination() {
            vendu(produitEnStock("RAPIDE", 10), 100, LocalDate.now());
            vendu(produitEnStock("MOYEN", 10), 50, LocalDate.now());
            vendu(produitEnStock("LENT", 10), 10, LocalDate.now());
            viderLeCache();

            assertThat(services.stockRotationReportService.getStockRotationPaginated(0, 2))
                .extracting(StockRotationDTO::libelle)
                .containsExactly("RAPIDE", "MOYEN");
            assertThat(services.stockRotationReportService.getStockRotationPaginated(1, 2))
                .extracting(StockRotationDTO::libelle)
                .containsExactly("LENT");
            assertThat(services.stockRotationReportService.getStockRotationCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("la pagination par catégorie respecte le filtre")
        void paginationParCategorie() {
            classe(produitEnStock("VITAL A", 10), ClasseCriticite.A_PLUS);
            classe(produitEnStock("VITAL B", 10), ClasseCriticite.A);
            classe(produitEnStock("OBSOLETE", 10), ClasseCriticite.D);
            viderLeCache();

            List<StockRotationDTO> page = services.stockRotationReportService.getStockRotationByABCPaginated(CategorieABC.A, 0, 1);

            assertThat(page).hasSize(1);
            assertThat(page.getFirst().categorieABC()).isEqualTo(CategorieABC.A);
        }
    }

    // ===== fabriques locales =====

    /** Un produit référencé et stocké au rayon : sans référencement, la vue n'a pas de prix d'achat. */
    private Produit produitEnStock(String libelle, int quantite) {
        Produit produit = produit(libelle, TypeProduit.PACKAGE, 10_000, 5);
        referencement(produit, fournisseur("LABOREX " + unique("")));
        stock(produit, quantite);
        return produit;
    }

    private void classe(Produit produit, ClasseCriticite classe) {
        produit.setClasseCriticite(classe);
        em.flush();
    }
}
