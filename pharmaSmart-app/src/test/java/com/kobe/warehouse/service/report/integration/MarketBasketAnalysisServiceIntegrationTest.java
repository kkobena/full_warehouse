package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.service.dto.report.MarketBasketSummaryDTO;
import com.kobe.warehouse.service.dto.report.ProductAssociationDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * L'analyse du panier cherche ce qui s'achète ensemble, pour décider de ce qu'on propose au comptoir
 * et de ce qu'on rapproche en rayon. Elle repose sur trois mesures que l'on confond facilement :
 *
 * <ul>
 *   <li>le <b>support</b> — la part de tous les tickets qui contiennent les deux produits ;
 *   <li>la <b>confiance</b> — parmi les tickets qui contiennent A, la part qui contient aussi B ;
 *   <li>le <b>lift</b> — le rapport de cette confiance à la fréquence de B dans l'ensemble.
 * </ul>
 *
 * <p>Le lift est le seul qui dise quelque chose : un produit acheté dans neuf tickets sur dix se
 * retrouvera avec une confiance élevée face à n'importe quoi, sans qu'il y ait association. Un lift
 * supérieur à un signale une vraie affinité ; inférieur à un, une répulsion. Confondre confiance et
 * lift conduit à mettre en avant le produit le plus banal du rayon.
 *
 * <p>Les jeux d'essai sont donc calibrés au centième : les trois mesures se vérifient à la main sur
 * quatre tickets.
 */
@DisplayName("MarketBasketAnalysisService — produits achetés ensemble")
class MarketBasketAnalysisServiceIntegrationTest extends AbstractReportIntegrationTest {

    private static final BigDecimal AUCUN_SEUIL = BigDecimal.ZERO;

    // ===== mesures =====

    @Nested
    @DisplayName("Mesures d'association")
    class Mesures {

        /**
         * Quatre tickets : A+B, A+B, A seul, C seul. Support = 2/4, confiance = 2/3, lift = la
         * confiance rapportée à la fréquence de B (2/4). Les trois se calculent de tête, et c'est
         * exactement ce qu'on vérifie.
         */
        @Test
        @DisplayName("support, confiance et lift se déduisent des tickets")
        void troisMesures() {
            Produit a = produitVendable("ANTALGIQUE");
            Produit b = produitVendable("BANDAGE");
            Produit c = produitVendable("CREME");

            panier(a, b);
            panier(a, b);
            panier(a);
            panier(c);
            viderLeCache();

            ProductAssociationDTO association = associations().getFirst();

            assertThat(association.productAName()).isEqualTo("ANTALGIQUE");
            assertThat(association.productBName()).isEqualTo("BANDAGE");
            assertThat(association.transactionsWithBoth()).isEqualTo(2L);
            assertThat(association.transactionsWithA()).isEqualTo(3L);
            assertThat(association.transactionsWithB()).isEqualTo(2L);
            assertThat(association.support()).isEqualByComparingTo("50.00");
            assertThat(association.confidence()).isEqualByComparingTo("66.67");
            assertThat(association.lift()).isEqualByComparingTo("1.3334");
        }

        /** Deux produits toujours achetés ensemble : confiance totale et lift maximal. */
        @Test
        @DisplayName("deux produits toujours achetés ensemble atteignent la confiance maximale")
        void associationParfaite() {
            Produit a = produitVendable("ANTALGIQUE");
            Produit b = produitVendable("BANDAGE");
            panier(a, b);
            panier(a, b);
            viderLeCache();

            ProductAssociationDTO association = associations().getFirst();

            assertThat(association.support()).isEqualByComparingTo("100.00");
            assertThat(association.confidence()).isEqualByComparingTo("100.00");
            assertThat(association.lift()).isEqualByComparingTo("1.0000");
        }

        @Test
        @DisplayName("un ticket à un seul produit ne crée aucune association")
        void ticketMonoProduitSansAssociation() {
            panier(produitVendable("SEUL"));
            viderLeCache();

            assertThat(associations()).isEmpty();
        }

        @Test
        @DisplayName("le code CIP de chaque produit accompagne l'association")
        void codesCipReportes() {
            Produit a = produitVendable("ANTALGIQUE");
            Produit b = produitVendable("BANDAGE");
            panier(a, b);
            viderLeCache();

            ProductAssociationDTO association = associations().getFirst();

            assertThat(association.productACodeCip()).isNotBlank();
            assertThat(association.productBCodeCip()).isNotBlank();
        }
    }

    // ===== seuils et bornes =====

    @Nested
    @DisplayName("Seuils et bornes")
    class SeuilsEtBornes {

        /** Le seuil de support écarte les rapprochements anecdotiques — deux tickets sur mille. */
        @Test
        @DisplayName("le seuil de support écarte les associations trop rares")
        void seuilDeSupport() {
            Produit a = produitVendable("ANTALGIQUE");
            Produit b = produitVendable("BANDAGE");
            Produit c = produitVendable("CREME");
            Produit d = produitVendable("DESINFECTANT");

            panier(a, b); // 1 ticket sur 4 → support 25 %
            panier(c);
            panier(c);
            panier(d);
            viderLeCache();

            assertThat(associationsAvecSeuils(BigDecimal.valueOf(20), AUCUN_SEUIL)).hasSize(1);
            assertThat(associationsAvecSeuils(BigDecimal.valueOf(30), AUCUN_SEUIL)).isEmpty();
        }

        @Test
        @DisplayName("le seuil de confiance écarte les associations peu probantes")
        void seuilDeConfiance() {
            Produit a = produitVendable("ANTALGIQUE");
            Produit b = produitVendable("BANDAGE");

            panier(a, b);
            panier(a);
            panier(a);
            panier(a); // confiance A→B = 1/4 = 25 %
            viderLeCache();

            assertThat(associationsAvecSeuils(AUCUN_SEUIL, BigDecimal.valueOf(20))).hasSize(1);
            assertThat(associationsAvecSeuils(AUCUN_SEUIL, BigDecimal.valueOf(30))).isEmpty();
        }

        @Test
        @DisplayName("la limite borne le nombre d'associations rendues")
        void limite() {
            Produit a = produitVendable("ANTALGIQUE");
            Produit b = produitVendable("BANDAGE");
            Produit c = produitVendable("CREME");
            panier(a, b, c);
            viderLeCache();

            // Trois paires possibles entre trois produits.
            assertThat(associations()).hasSize(3);
            assertThat(
                services.marketBasketAnalysisService.getProductAssociations(
                    ilYA(30),
                    LocalDate.now(),
                    AUCUN_SEUIL,
                    AUCUN_SEUIL,
                    2
                )
            ).hasSize(2);
        }

        @Test
        @DisplayName("les associations les plus fréquentes passent en tête")
        void ordreParFrequence() {
            Produit a = produitVendable("ANTALGIQUE");
            Produit b = produitVendable("BANDAGE");
            Produit c = produitVendable("CREME");

            panier(a, b);
            panier(a, b);
            panier(a, c);
            viderLeCache();

            assertThat(associations().getFirst().transactionsWithBoth()).isEqualTo(2L);
        }
    }

    // ===== périmètre =====

    @Nested
    @DisplayName("Périmètre")
    class Perimetre {

        @Test
        @DisplayName("un ticket annulé ne crée aucune association")
        void ticketAnnuleExclu() {
            Produit a = produitVendable("ANTALGIQUE");
            Produit b = produitVendable("BANDAGE");
            panierAnnule(a, b);
            viderLeCache();

            assertThat(associations()).isEmpty();
        }

        @Test
        @DisplayName("un ticket hors période n'entre pas dans l'analyse")
        void ticketHorsPeriodeExclu() {
            Produit a = produitVendable("ANTALGIQUE");
            Produit b = produitVendable("BANDAGE");
            panier(LocalDate.now().minusDays(60), a, b);
            viderLeCache();

            assertThat(associations()).isEmpty();
        }

        @Test
        @DisplayName("base vierge : aucune association")
        void baseVierge() {
            assertThat(associations()).isEmpty();
        }
    }

    // ===== associations d'un produit =====

    @Nested
    @DisplayName("Associations d'un produit")
    class AssociationsDUnProduit {

        /** C'est la question du comptoir : « avec ça, que propose-t-on ? » */
        @Test
        @DisplayName("les produits achetés avec un produit donné, du plus fréquent au moins fréquent")
        void produitsAssocies() {
            Produit a = produitVendable("ANTALGIQUE");
            Produit b = produitVendable("BANDAGE");
            Produit c = produitVendable("CREME");

            panier(a, b);
            panier(a, b);
            panier(a, c);
            viderLeCache();

            List<ProductAssociationDTO> associes = services.marketBasketAnalysisService.getAssociationsForProduct(
                a.getId().longValue(),
                ilYA(30),
                LocalDate.now(),
                10
            );

            assertThat(associes).extracting(ProductAssociationDTO::productBName).containsExactly("BANDAGE", "CREME");
            assertThat(associes.getFirst().productAName()).isEqualTo("ANTALGIQUE");
            assertThat(associes.getFirst().transactionsWithBoth()).isEqualTo(2L);
        }

        @Test
        @DisplayName("un produit jamais accompagné n'a aucune association")
        void produitSansAssociation() {
            Produit seul = produitVendable("SEUL");
            panier(seul);
            viderLeCache();

            assertThat(
                services.marketBasketAnalysisService.getAssociationsForProduct(
                    seul.getId().longValue(),
                    ilYA(30),
                    LocalDate.now(),
                    10
                )
            ).isEmpty();
        }

        /** Les recommandations de vente croisée regardent les six derniers mois. */
        @Test
        @DisplayName("les recommandations couvrent les six derniers mois")
        void recommandationsSurSixMois() {
            Produit a = produitVendable("ANTALGIQUE");
            Produit b = produitVendable("BANDAGE");
            panier(LocalDate.now().minusMonths(3), a, b);
            panier(LocalDate.now().minusMonths(9), a, produitVendable("ANCIEN"));
            viderLeCache();

            assertThat(services.marketBasketAnalysisService.getCrossSellRecommendations(a.getId().longValue()))
                .extracting(ProductAssociationDTO::productBName)
                .containsExactly("BANDAGE");
        }
    }

    // ===== synthèse =====

    @Nested
    @DisplayName("Synthèse")
    class Synthese {

        @Test
        @DisplayName("la synthèse totalise tickets, produits et taille moyenne du panier")
        void totaux() {
            Produit a = produitVendable("ANTALGIQUE");
            Produit b = produitVendable("BANDAGE");
            Produit c = produitVendable("CREME");

            panier(a, b);
            panier(a, b);
            panier(a);
            panier(c);
            viderLeCache();

            MarketBasketSummaryDTO synthese = services.marketBasketAnalysisService.getMarketBasketSummary(ilYA(30), LocalDate.now());

            assertThat(synthese.totalTransactions()).isEqualTo(4L);
            assertThat(synthese.totalProducts()).isEqualTo(3L);
            assertThat(synthese.averageBasketSize()).isEqualByComparingTo("1.50");
        }

        @Test
        @DisplayName("la paire la plus fréquente est nommée")
        void paireLaPlusFrequente() {
            Produit a = produitVendable("ANTALGIQUE");
            Produit b = produitVendable("BANDAGE");
            panier(a, b);
            panier(a, b);
            viderLeCache();

            MarketBasketSummaryDTO synthese = services.marketBasketAnalysisService.getMarketBasketSummary(ilYA(30), LocalDate.now());

            assertThat(synthese.mostFrequentPair()).isEqualTo("ANTALGIQUE + BANDAGE");
            assertThat(synthese.totalAssociations()).isEqualTo(1L);
        }

        /**
         * Une officine sans vente sur la période — un jour férié, une ouverture récente — ne doit pas
         * faire tomber l'écran : la moyenne d'un ensemble vide n'est pas zéro, elle n'existe pas, et
         * c'est au service de le traduire.
         */
        @Test
        @DisplayName("sans aucun ticket, la synthèse rend des zéros plutôt qu'une erreur")
        void baseVierge() {
            assertThatCode(() -> {
                MarketBasketSummaryDTO synthese = services.marketBasketAnalysisService.getMarketBasketSummary(
                    ilYA(30),
                    LocalDate.now()
                );

                assertThat(synthese.totalTransactions()).isZero();
                assertThat(synthese.totalProducts()).isZero();
                assertThat(synthese.totalAssociations()).isZero();
                assertThat(synthese.averageBasketSize()).isEqualByComparingTo("0");
                assertThat(synthese.mostFrequentPair()).isEqualTo("Aucune");
            }).doesNotThrowAnyException();
        }
    }

    // ===== fabriques locales =====

    private static LocalDate ilYA(int jours) {
        return LocalDate.now().minusDays(jours);
    }

    private List<ProductAssociationDTO> associations() {
        return associationsAvecSeuils(AUCUN_SEUIL, AUCUN_SEUIL);
    }

    private List<ProductAssociationDTO> associationsAvecSeuils(BigDecimal support, BigDecimal confiance) {
        return services.marketBasketAnalysisService.getProductAssociations(ilYA(30), LocalDate.now(), support, confiance, 50);
    }

    private Produit produitVendable(String libelle) {
        Produit produit = produit(libelle, TypeProduit.PACKAGE, 10_000, 5);
        referencement(produit, fournisseur("LABOREX " + unique("")));
        return produit;
    }

    /** Un ticket portant plusieurs produits — c'est le panier que l'analyse examine. */
    private void panier(Produit... produits) {
        panier(LocalDate.now(), produits);
    }

    private void panier(LocalDate date, Produit... produits) {
        Sales vente = venteFermee(date, false, CategorieChiffreAffaire.CA);
        for (Produit produit : produits) {
            ligneDeVente(vente, produit, 1);
        }
        em.flush();
    }

    private void panierAnnule(Produit... produits) {
        Sales vente = venteFermee(LocalDate.now(), true, CategorieChiffreAffaire.CA);
        for (Produit produit : produits) {
            ligneDeVente(vente, produit, 1);
        }
        em.flush();
    }
}
