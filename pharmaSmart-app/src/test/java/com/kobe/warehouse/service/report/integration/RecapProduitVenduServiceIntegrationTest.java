package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.service.stock.dto.RecapProduitVendu;
import com.kobe.warehouse.service.stock.dto.RecapProduitVenduRequestParam;
import com.kobe.warehouse.service.stock.dto.RecapProduitVenduSummary;
import com.kobe.warehouse.service.stock.dto.SeuilFilterType;
import com.kobe.warehouse.service.stock.dto.StockFilterType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Le récapitulatif des produits vendus est l'écran d'analyse le plus ouvert du logiciel : une
 * dizaine de filtres combinables — période, heure, caissier, rayon, fournisseur, seuil, stock — dont
 * le SQL est assemblé morceau par morceau à l'exécution. Chaque combinaison produit une requête qui
 * n'a peut-être jamais été exécutée, et un filtre qui ne filtre pas ne se voit pas : l'écran affiche
 * simplement plus de lignes que demandé.
 *
 * <p>Deux pièges structurent le rapport. Le premier est le <b>produit cartésien</b> : un produit
 * référencé chez trois fournisseurs et rangé dans deux rayons apparaît six fois dans les jointures,
 * et sommer naïvement ses quantités les multiplierait par six. C'est pourquoi le stock passe par une
 * sous-requête corrélée plutôt que par une jointure. Le second est le <b>pendant invendu</b> : la
 * liste de ce qui ne s'est pas vendu se construit par la négative, et c'est l'accord entre les deux
 * écrans — ce qui est dans l'un n'est pas dans l'autre — qui fait qu'on peut s'y fier.
 */
@DisplayName("RecapProduitVenduService — récapitulatif des produits vendus")
class RecapProduitVenduServiceIntegrationTest extends AbstractReportIntegrationTest {

    private static final Pageable PAGE = PageRequest.of(0, 50);

    // ===== agrégation =====

    @Nested
    @DisplayName("Agrégation par produit")
    class Agregation {

        @Test
        @DisplayName("totalise quantités, chiffre d'affaires et coût d'achat de chaque produit")
        void totauxParProduit() {
            Produit produit = produitVendable("DOLIPRANE", 10_000, 6_000);
            stock(produit, 25);
            vendu(produit, 3, LocalDate.now());
            vendu(produit, 7, LocalDate.now());
            viderLeCache();

            RecapProduitVendu ligne = recap().getFirst();

            assertThat(ligne.libelle()).isEqualTo("DOLIPRANE");
            assertThat(ligne.quantitySold()).isEqualTo(10);
            assertThat(ligne.totalSalesAmount()).isEqualTo(100_000);
            assertThat(ligne.totalPurchaseAmount()).isEqualTo(60_000);
            assertThat(ligne.stock()).isEqualTo(25);
        }

        /**
         * Trois référencements fournisseurs et deux rayons font six lignes de jointure pour un seul
         * produit. Sans la sous-requête corrélée, le stock serait sextuplé — et l'écran annoncerait
         * un stock qui n'existe pas.
         */
        @Test
        @DisplayName("les jointures multiples ne multiplient ni le stock ni les quantités")
        void pasDeProduitCartesien() {
            Produit produit = produitVendable("DOLIPRANE", 10_000, 6_000);
            stock(produit, 25);
            referencement(produit, fournisseur("DPCI " + unique("")));
            referencement(produit, fournisseur("COPHARMED " + unique("")));
            rangeAuRayon(produit, "COMPTOIR");
            rangeAuRayon(produit, "VITRINE");
            vendu(produit, 10, LocalDate.now());
            viderLeCache();

            List<RecapProduitVendu> lignes = recap();

            assertThat(lignes).hasSize(1);
            assertThat(lignes.getFirst().quantitySold()).isEqualTo(10);
            assertThat(lignes.getFirst().stock()).isEqualTo(25);
            assertThat(lignes.getFirst().totalSalesAmount()).isEqualTo(100_000);
        }

        @Test
        @DisplayName("les produits les plus vendus en valeur passent en tête")
        void ordreParChiffreDAffaires() {
            vendu(produitVendable("PETIT", 1_000, 600), 1, LocalDate.now());
            vendu(produitVendable("GROS", 100_000, 60_000), 10, LocalDate.now());
            vendu(produitVendable("MOYEN", 10_000, 6_000), 5, LocalDate.now());
            viderLeCache();

            assertThat(recap()).extracting(RecapProduitVendu::libelle).containsExactly("GROS", "MOYEN", "PETIT");
        }

        @Test
        @DisplayName("la pagination borne les lignes et rapporte le total")
        void pagination() {
            vendu(produitVendable("A", 100_000, 60_000), 1, LocalDate.now());
            vendu(produitVendable("B", 10_000, 6_000), 1, LocalDate.now());
            vendu(produitVendable("C", 1_000, 600), 1, LocalDate.now());
            viderLeCache();

            Page<RecapProduitVendu> page = services.recapProduitVenduService.getRecapProduitVenduReport(
                parametres().build(),
                PageRequest.of(0, 2)
            );

            assertThat(page.getContent()).extracting(RecapProduitVendu::libelle).containsExactly("A", "B");
            assertThat(page.getTotalElements()).isEqualTo(3);
        }

        @Test
        @DisplayName("base vierge : aucune ligne, aucun total")
        void baseVierge() {
            assertThat(recap()).isEmpty();

            RecapProduitVenduSummary resume = services.recapProduitVenduService.getRecapProduitVenduSummary(parametres().build());

            assertThat(resume.totalProducts()).isZero();
            assertThat(resume.quantitySold()).isZero();
            assertThat(resume.totalSalesAmount()).isZero();
        }
    }

    // ===== filtres =====

    @Nested
    @DisplayName("Filtres")
    class Filtres {

        @Test
        @DisplayName("la période borne les ventes prises en compte")
        void filtreParPeriode() {
            Produit produit = produitVendable("DOLIPRANE", 10_000, 6_000);
            vendu(produit, 3, LocalDate.now().minusDays(10));
            vendu(produit, 7, LocalDate.now());
            viderLeCache();

            RecapProduitVenduRequestParam duJour = parametres().du(LocalDate.now()).au(LocalDate.now()).build();

            assertThat(services.recapProduitVenduService.getRecapProduitVenduReport(duJour, PAGE).getContent().getFirst().quantitySold())
                .isEqualTo(7);
        }

        /**
         * Les heures ne découpent pas chaque journée : elles précisent les deux bornes de la période,
         * du premier jour à telle heure au dernier jour à telle autre. Sur une seule journée, cela
         * revient bien à isoler un créneau — une garde de nuit, une relève d'équipe — et c'est ainsi
         * que l'écran s'en sert. Le filtre porte sur l'horodatage de la ligne de vente, pas sur la
         * date de la vente.
         */
        @Test
        @DisplayName("les heures précisent les bornes de la période, à la minute près")
        void filtreParHeure() {
            Produit produit = produitVendable("DOLIPRANE", 10_000, 6_000);
            vendu(produit, 5, LocalDate.now()); // la fabrique horodate les ventes vers 10 h
            viderLeCache();

            RecapProduitVenduRequestParam journeeEntiere = parametres()
                .du(LocalDate.now())
                .au(LocalDate.now())
                .de(LocalTime.of(0, 0))
                .a(LocalTime.of(23, 59))
                .build();
            RecapProduitVenduRequestParam soireeSeule = parametres()
                .du(LocalDate.now())
                .au(LocalDate.now())
                .de(LocalTime.of(20, 0))
                .a(LocalTime.of(23, 59))
                .build();

            assertThat(services.recapProduitVenduService.getRecapProduitVenduReport(journeeEntiere, PAGE).getContent()).hasSize(1);
            assertThat(services.recapProduitVenduService.getRecapProduitVenduReport(soireeSeule, PAGE).getContent()).isEmpty();
        }

        @Test
        @DisplayName("le filtre par caissier ne retient que ses ventes")
        void filtreParCaissier() {
            vendu(produitVendable("DOLIPRANE", 10_000, 6_000), 5, LocalDate.now());
            viderLeCache();

            assertThat(
                services.recapProduitVenduService
                    .getRecapProduitVenduReport(parametres().caissier(utilisateur.getId()).build(), PAGE)
                    .getContent()
            ).hasSize(1);
            assertThat(
                services.recapProduitVenduService.getRecapProduitVenduReport(parametres().caissier(999_999).build(), PAGE).getContent()
            ).isEmpty();
        }

        @Test
        @DisplayName("la recherche porte sur le libellé et sur les codes du produit")
        void recherche() {
            vendu(produitVendable("DOLIPRANE 1000", 10_000, 6_000), 5, LocalDate.now());
            vendu(produitVendable("EFFERALGAN", 10_000, 6_000), 5, LocalDate.now());
            viderLeCache();

            assertThat(
                services.recapProduitVenduService.getRecapProduitVenduReport(parametres().recherche("dolipr").build(), PAGE).getContent()
            )
                .extracting(RecapProduitVendu::libelle)
                .containsExactly("DOLIPRANE 1000");
        }

        @Test
        @DisplayName("le filtre par rayon ne retient que les produits qui y sont rangés")
        void filtreParRayon() {
            Produit auComptoir = produitVendable("AU COMPTOIR", 10_000, 6_000);
            Rayon comptoir = rangeAuRayon(auComptoir, "COMPTOIR");
            vendu(auComptoir, 5, LocalDate.now());

            Produit ailleurs = produitVendable("AILLEURS", 10_000, 6_000);
            rangeAuRayon(ailleurs, "VITRINE");
            vendu(ailleurs, 5, LocalDate.now());
            viderLeCache();

            assertThat(
                services.recapProduitVenduService
                    .getRecapProduitVenduReport(parametres().rayon(comptoir.getId()).build(), PAGE)
                    .getContent()
            )
                .extracting(RecapProduitVendu::libelle)
                .containsExactly("AU COMPTOIR");
        }

        @Test
        @DisplayName("le filtre par fournisseur suit le référencement du produit")
        void filtreParFournisseur() {
            Produit produit = produit("DOLIPRANE", TypeProduit.PACKAGE, 10_000, 5);
            Fournisseur laborex = fournisseur("LABOREX " + unique(""));
            referencement(produit, laborex);
            vendu(produit, 5, LocalDate.now());

            vendu(produitVendable("AUTRE", 10_000, 6_000), 5, LocalDate.now());
            viderLeCache();

            assertThat(
                services.recapProduitVenduService
                    .getRecapProduitVenduReport(parametres().fournisseur(laborex.getId()).build(), PAGE)
                    .getContent()
            )
                .extracting(RecapProduitVendu::libelle)
                .containsExactly("DOLIPRANE");
        }

        /** Vendre en dessous du prix d'achat est une anomalie de tarification : c'est ce filtre qui la débusque. */
        @Test
        @DisplayName("le filtre repère les lignes vendues sous le prix d'achat")
        void filtreVenteAPerte() {
            Produit aPerte = produitVendable("A PERTE", 5_000, 8_000);
            vendu(aPerte, 5, LocalDate.now());
            vendu(produitVendable("NORMAL", 10_000, 6_000), 5, LocalDate.now());
            viderLeCache();

            assertThat(
                services.recapProduitVenduService
                    .getRecapProduitVenduReport(parametres().sousLePrixDAchat().build(), PAGE)
                    .getContent()
            )
                .extracting(RecapProduitVendu::libelle)
                .containsExactly("A PERTE");
        }

        @Test
        @DisplayName("le filtre par quantité vendue porte sur la ligne de vente")
        void filtreParQuantiteVendue() {
            vendu(produitVendable("TROIS", 10_000, 6_000), 3, LocalDate.now());
            vendu(produitVendable("SEPT", 10_000, 6_000), 7, LocalDate.now());
            viderLeCache();

            assertThat(
                services.recapProduitVenduService.getRecapProduitVenduReport(parametres().quantiteVendue(3).build(), PAGE).getContent()
            )
                .extracting(RecapProduitVendu::libelle)
                .containsExactly("TROIS");
        }
    }

    // ===== filtres de seuil et de stock =====

    @Nested
    @DisplayName("Filtres de seuil et de stock")
    class SeuilEtStock {

        @Test
        @DisplayName("le seuil mini se compare dans les deux sens")
        void filtreParSeuil() {
            Produit bas = produitVendable("SEUIL BAS", 10_000, 6_000, 2);
            Produit haut = produitVendable("SEUIL HAUT", 10_000, 6_000, 50);
            vendu(bas, 5, LocalDate.now());
            vendu(haut, 5, LocalDate.now());
            viderLeCache();

            assertThat(avecSeuil(SeuilFilterType.LESS_THAN, 10)).containsExactly("SEUIL BAS");
            assertThat(avecSeuil(SeuilFilterType.GREATER_THAN, 10)).containsExactly("SEUIL HAUT");
            assertThat(avecSeuil(SeuilFilterType.EQUAL_TO, 2)).containsExactly("SEUIL BAS");
            assertThat(avecSeuil(SeuilFilterType.GREATER_THAN_OR_EQUAL_TO, 50)).containsExactly("SEUIL HAUT");
            assertThat(avecSeuil(SeuilFilterType.LESS_THAN_OR_EQUAL_TO, 2)).containsExactly("SEUIL BAS");
        }

        /**
         * « Seuil mini atteint » ne compare pas à une valeur saisie mais au seuil propre à chaque
         * produit : il passe donc par un {@code HAVING}, et non par le {@code WHERE}.
         */
        @Test
        @DisplayName("« seuil mini atteint » compare le stock au seuil propre de chaque produit")
        void seuilMiniAtteint() {
            Produit suffisant = produitVendable("SUFFISANT", 10_000, 6_000, 10);
            stock(suffisant, 50);
            vendu(suffisant, 5, LocalDate.now());

            Produit insuffisant = produitVendable("INSUFFISANT", 10_000, 6_000, 100);
            stock(insuffisant, 5);
            vendu(insuffisant, 5, LocalDate.now());
            viderLeCache();

            RecapProduitVenduRequestParam param = parametres().seuil(SeuilFilterType.SEUIL_MINI_ATTEINT, null).build();
            Page<RecapProduitVendu> page = services.recapProduitVenduService.getRecapProduitVenduReport(param, PAGE);

            assertThat(page.getContent()).extracting(RecapProduitVendu::libelle).containsExactly("SUFFISANT");
            assertThat(page.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("le stock se compare dans les deux sens")
        void filtreParStock() {
            Produit peu = produitVendable("PEU DE STOCK", 10_000, 6_000);
            stock(peu, 3);
            vendu(peu, 1, LocalDate.now());

            Produit beaucoup = produitVendable("BEAUCOUP DE STOCK", 10_000, 6_000);
            stock(beaucoup, 300);
            vendu(beaucoup, 1, LocalDate.now());
            viderLeCache();

            assertThat(avecStock(StockFilterType.LESS_THAN, 10)).containsExactly("PEU DE STOCK");
            assertThat(avecStock(StockFilterType.GREATER_THAN, 10)).containsExactly("BEAUCOUP DE STOCK");
            assertThat(avecStock(StockFilterType.EQUAL_TO, 3)).containsExactly("PEU DE STOCK");
            assertThat(avecStock(StockFilterType.NOT_EQUAL_TO, 3)).containsExactly("BEAUCOUP DE STOCK");
        }

        /**
         * « Rupture de stock » est proposé à l'écran sans champ de valeur — il n'a pas de nombre à
         * comparer, il désigne le stock épuisé. Le filtre doit donc s'appliquer sur son seul intitulé.
         */
        @Test
        @DisplayName("« rupture de stock » ne retient que les produits épuisés")
        void filtreRuptureDeStock() {
            Produit epuise = produitVendable("EPUISE", 10_000, 6_000);
            stock(epuise, 0);
            vendu(epuise, 1, LocalDate.now());

            Produit approvisionne = produitVendable("APPROVISIONNE", 10_000, 6_000);
            stock(approvisionne, 50);
            vendu(approvisionne, 1, LocalDate.now());
            viderLeCache();

            RecapProduitVenduRequestParam param = parametres().stock(StockFilterType.OUT_OF_STOCK, null).build();

            assertThat(services.recapProduitVenduService.getRecapProduitVenduReport(param, PAGE).getContent())
                .extracting(RecapProduitVendu::libelle)
                .containsExactly("EPUISE");
        }

        private List<String> avecSeuil(SeuilFilterType type, int valeur) {
            return services.recapProduitVenduService
                .getRecapProduitVenduReport(parametres().seuil(type, valeur).build(), PAGE)
                .getContent()
                .stream()
                .map(RecapProduitVendu::libelle)
                .toList();
        }

        private List<String> avecStock(StockFilterType type, int valeur) {
            return services.recapProduitVenduService
                .getRecapProduitVenduReport(parametres().stock(type, valeur).build(), PAGE)
                .getContent()
                .stream()
                .map(RecapProduitVendu::libelle)
                .toList();
        }
    }

    // ===== invendus =====

    @Nested
    @DisplayName("Invendus")
    class Invendus {

        /** Les deux écrans sont complémentaires : un produit est dans l'un ou dans l'autre, jamais dans les deux. */
        @Test
        @DisplayName("un produit vendu sur la période n'est pas un invendu, et réciproquement")
        void complementariteDesDeuxEcrans() {
            Produit vendu = produitVendable("VENDU", 10_000, 6_000);
            stock(vendu, 10);
            vendu(vendu, 5, LocalDate.now());

            Produit dormant = produitVendable("DORMANT", 10_000, 6_000);
            stock(dormant, 40);
            viderLeCache();

            assertThat(recap()).extracting(RecapProduitVendu::libelle).containsExactly("VENDU");
            assertThat(invendus()).extracting(RecapProduitVendu::libelle).containsExactly("DORMANT");
        }

        /**
         * L'invendu n'a pas de chiffre d'affaires réalisé : ce que le rapport valorise est son stock
         * dormant, au prix de vente et au prix d'achat. C'est le montant que l'officine a immobilisé
         * sans rien vendre.
         */
        @Test
        @DisplayName("l'invendu est valorisé au stock dormant, pas à des ventes")
        void valorisationDuStockDormant() {
            Produit dormant = produitVendable("DORMANT", 10_000, 6_000);
            stock(dormant, 40);
            viderLeCache();

            RecapProduitVendu ligne = invendus().getFirst();

            assertThat(ligne.quantitySold()).isZero();
            assertThat(ligne.stock()).isEqualTo(40);
            assertThat(ligne.totalSalesAmount()).isEqualTo(400_000);
            assertThat(ligne.totalPurchaseAmount()).isEqualTo(240_000);
        }

        @Test
        @DisplayName("un produit vendu hors période redevient un invendu de la période")
        void venduHorsPeriodeEstInvendu() {
            Produit produit = produitVendable("VENDU AVANT", 10_000, 6_000);
            stock(produit, 10);
            vendu(produit, 5, LocalDate.now().minusDays(30));
            viderLeCache();

            RecapProduitVenduRequestParam duJour = parametres().du(LocalDate.now()).au(LocalDate.now()).build();

            assertThat(services.recapProduitVenduService.getRecapProduitInvenduReport(duJour, PAGE).getContent())
                .extracting(RecapProduitVendu::libelle)
                .containsExactly("VENDU AVANT");
        }

        @Test
        @DisplayName("les invendus sont classés par libellé")
        void ordreAlphabetique() {
            stock(produitVendable("ZEBRE", 10_000, 6_000), 1);
            stock(produitVendable("ALPHA", 10_000, 6_000), 1);
            viderLeCache();

            assertThat(invendus()).extracting(RecapProduitVendu::libelle).containsExactly("ALPHA", "ZEBRE");
        }

        @Test
        @DisplayName("le résumé des invendus totalise le stock immobilisé")
        void resumeDesInvendus() {
            stock(produitVendable("DORMANT A", 10_000, 6_000), 10);
            stock(produitVendable("DORMANT B", 10_000, 6_000), 30);
            viderLeCache();

            RecapProduitVenduSummary resume = services.recapProduitVenduService.getRecapProduitInvenduSummary(parametres().build());

            assertThat(resume.totalProducts()).isEqualTo(2);
            assertThat(resume.totalStock()).isEqualTo(40L);
            assertThat(resume.quantitySold()).isZero();
        }
    }

    // ===== résumé =====

    @Nested
    @DisplayName("Résumé")
    class Resume {

        @Test
        @DisplayName("le résumé totalise produits, quantités, chiffre d'affaires et stock")
        void totaux() {
            Produit a = produitVendable("A", 10_000, 6_000);
            stock(a, 15);
            vendu(a, 4, LocalDate.now());

            Produit b = produitVendable("B", 20_000, 12_000);
            stock(b, 25);
            vendu(b, 2, LocalDate.now());
            viderLeCache();

            RecapProduitVenduSummary resume = services.recapProduitVenduService.getRecapProduitVenduSummary(parametres().build());

            assertThat(resume.totalProducts()).isEqualTo(2);
            assertThat(resume.quantitySold()).isEqualTo(6);
            assertThat(resume.totalSalesAmount()).isEqualTo(80_000L); // 40 000 + 40 000
            assertThat(resume.totalPurchaseAmount()).isEqualTo(48_000L); // 24 000 + 24 000
            assertThat(resume.totalStock()).isEqualTo(40L);
        }

        /** Le résumé passe par la même sous-requête corrélée : il ne doit pas non plus démultiplier. */
        @Test
        @DisplayName("le résumé ne démultiplie pas le stock d'un produit multi-référencé")
        void resumeSansProduitCartesien() {
            Produit produit = produitVendable("DOLIPRANE", 10_000, 6_000);
            stock(produit, 25);
            referencement(produit, fournisseur("DPCI " + unique("")));
            rangeAuRayon(produit, "COMPTOIR");
            rangeAuRayon(produit, "VITRINE");
            vendu(produit, 10, LocalDate.now());
            viderLeCache();

            RecapProduitVenduSummary resume = services.recapProduitVenduService.getRecapProduitVenduSummary(parametres().build());

            assertThat(resume.totalProducts()).isEqualTo(1);
            assertThat(resume.quantitySold()).isEqualTo(10);
            assertThat(resume.totalStock()).isEqualTo(25L);
        }

        @Test
        @DisplayName("le résumé suit les mêmes filtres que la liste")
        void resumeFiltre() {
            Produit produit = produitVendable("DOLIPRANE", 10_000, 6_000);
            vendu(produit, 3, LocalDate.now().minusDays(10));
            vendu(produit, 7, LocalDate.now());
            viderLeCache();

            RecapProduitVenduRequestParam duJour = parametres().du(LocalDate.now()).au(LocalDate.now()).build();

            assertThat(services.recapProduitVenduService.getRecapProduitVenduSummary(duJour).quantitySold()).isEqualTo(7);
        }
    }

    // ===== périmètre =====

    @Nested
    @DisplayName("Périmètre")
    class Perimetre {

        @Test
        @DisplayName("une vente annulée ne figure pas au récapitulatif")
        void venteAnnuleeExclue() {
            Produit produit = produitVendable("DOLIPRANE", 10_000, 6_000);
            vendu(produit, 10, LocalDate.now(), true, CategorieChiffreAffaire.CA);
            viderLeCache();

            assertThat(recap()).isEmpty();
        }

        /**
         * Contrairement aux vues d'analyse, le récapitulatif ne filtre pas sur la catégorie de
         * chiffre d'affaires : c'est un état de mouvement de marchandise, pas une déclaration.
         */
        @Test
        @DisplayName("une vente dépôt figure au récapitulatif — c'est un mouvement de marchandise")
        void venteHorsChiffreDAffairesIncluse() {
            Produit produit = produitVendable("DOLIPRANE", 10_000, 6_000);
            vendu(produit, 10, LocalDate.now(), false, CategorieChiffreAffaire.CA_DEPOT);
            viderLeCache();

            assertThat(recap()).extracting(RecapProduitVendu::quantitySold).containsExactly(10);
        }
    }

    // ===== fabriques locales =====

    private List<RecapProduitVendu> recap() {
        return services.recapProduitVenduService.getRecapProduitVenduReport(parametres().build(), PAGE).getContent();
    }

    private List<RecapProduitVendu> invendus() {
        return services.recapProduitVenduService.getRecapProduitInvenduReport(parametres().build(), PAGE).getContent();
    }

    private Produit produitVendable(String libelle, int prixVente, int coutAchat) {
        return produitVendable(libelle, prixVente, coutAchat, 5);
    }

    private Produit produitVendable(String libelle, int prixVente, int coutAchat, int seuilMini) {
        Produit produit = produit(libelle, TypeProduit.PACKAGE, prixVente, seuilMini);
        produit.setCostAmount(coutAchat);
        produit.setItemCostAmount(coutAchat);
        em.flush();
        referencement(produit, fournisseur("LABOREX " + unique("")));
        return produit;
    }

    private ParametresBuilder parametres() {
        return new ParametresBuilder();
    }

    /** Les paramètres du rapport ont vingt-deux champs : seuls ceux qui comptent sont nommés ici. */
    private static final class ParametresBuilder {

        private LocalDate debut = LocalDate.now().minusYears(1);
        private LocalDate fin = LocalDate.now();
        private LocalTime heureDebut;
        private LocalTime heureFin;
        private Integer caissierId;
        private String recherche;
        private Integer rayonId;
        private Integer fournisseurId;
        private SeuilFilterType typeSeuil;
        private StockFilterType typeStock;
        private Integer valeurSeuil;
        private Integer valeurStock;
        private Integer quantiteVendue;
        private Boolean sousLePrixDAchat;

        ParametresBuilder du(LocalDate debut) {
            this.debut = debut;
            return this;
        }

        ParametresBuilder au(LocalDate fin) {
            this.fin = fin;
            return this;
        }

        ParametresBuilder de(LocalTime heureDebut) {
            this.heureDebut = heureDebut;
            return this;
        }

        ParametresBuilder a(LocalTime heureFin) {
            this.heureFin = heureFin;
            return this;
        }

        ParametresBuilder caissier(Integer caissierId) {
            this.caissierId = caissierId;
            return this;
        }

        ParametresBuilder recherche(String recherche) {
            this.recherche = recherche;
            return this;
        }

        ParametresBuilder rayon(Integer rayonId) {
            this.rayonId = rayonId;
            return this;
        }

        ParametresBuilder fournisseur(Integer fournisseurId) {
            this.fournisseurId = fournisseurId;
            return this;
        }

        ParametresBuilder seuil(SeuilFilterType type, Integer valeur) {
            this.typeSeuil = type;
            this.valeurSeuil = valeur;
            return this;
        }

        ParametresBuilder stock(StockFilterType type, Integer valeur) {
            this.typeStock = type;
            this.valeurStock = valeur;
            return this;
        }

        ParametresBuilder quantiteVendue(Integer quantite) {
            this.quantiteVendue = quantite;
            return this;
        }

        ParametresBuilder sousLePrixDAchat() {
            this.sousLePrixDAchat = true;
            return this;
        }

        RecapProduitVenduRequestParam build() {
            return new RecapProduitVenduRequestParam(
                debut,
                fin,
                heureDebut,
                heureFin,
                caissierId,
                recherche,
                rayonId,
                fournisseurId,
                typeSeuil,
                typeStock,
                valeurSeuil,
                valeurStock,
                quantiteVendue,
                sousLePrixDAchat,
                null,
                null,
                null,
                null,
                null,
                null,
                false
            );
        }
    }
}
