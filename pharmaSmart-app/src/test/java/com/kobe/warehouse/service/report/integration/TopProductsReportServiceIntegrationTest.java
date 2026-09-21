package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.service.dto.report.TopProductDTO;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Le palmarès mensuel répond à deux questions qui n'ont pas la même réponse : qu'est-ce qui
 * <b>rapporte</b> le plus, et qu'est-ce qui <b>sort</b> le plus. Un générique à petit prix domine
 * les quantités sans peser au chiffre d'affaires ; une spécialité coûteuse fait l'inverse. Les deux
 * classements servent à des décisions différentes — la négociation d'achat pour l'un, le
 * réapprovisionnement pour l'autre.
 *
 * <p>La vue {@code mv_monthly_top_products} range par mois, sur six mois glissants. Son mois est
 * stocké en <b>texte</b>, pas en date : le service lui passe donc une chaîne, et cet accord — texte
 * contre texte — ne se vérifie qu'en l'exécutant. Une requête qui comparerait une date à ce texte
 * échouerait, ou pire, ne rendrait rien.
 *
 * <p>Le service normalise ce qu'on lui donne : quelle que soit la date reçue, il interroge le
 * premier jour de son mois. C'est ce qui permet à l'écran d'envoyer « aujourd'hui » et d'obtenir le
 * mois en cours.
 */
@DisplayName("TopProductsReportService — palmarès mensuel sur mv_monthly_top_products")
class TopProductsReportServiceIntegrationTest extends AbstractReportIntegrationTest {

    private static final String VUE = "mv_monthly_top_products";

    // ===== classements =====

    @Nested
    @DisplayName("Classements")
    class Classements {

        /**
         * Les deux classements se séparent ici : le produit cher domine le chiffre d'affaires, le
         * produit bon marché domine les quantités. Les confondre reviendrait à négocier l'achat du
         * mauvais produit.
         */
        @Test
        @DisplayName("le classement par chiffre d'affaires et celui par quantité ne donnent pas le même ordre")
        void deuxClassementsDistincts() {
            vendu(produitReference("CHER", 100_000), 10, LocalDate.now()); // CA 1 000 000, qté 10
            vendu(produitReference("BON MARCHE", 1_000), 500, LocalDate.now()); // CA 500 000, qté 500
            rafraichir(VUE);

            assertThat(services.topProductsReportService.getTopProductsByRevenue(LocalDate.now(), 10))
                .extracting(TopProductDTO::libelle)
                .containsExactly("CHER", "BON MARCHE");

            assertThat(services.topProductsReportService.getTopProductsByQuantity(LocalDate.now(), 10))
                .extracting(TopProductDTO::libelle)
                .containsExactly("BON MARCHE", "CHER");
        }

        @Test
        @DisplayName("la limite du palmarès est respectée")
        void limiteRespectee() {
            vendu(produitReference("A", 100_000), 10, LocalDate.now());
            vendu(produitReference("B", 10_000), 10, LocalDate.now());
            vendu(produitReference("C", 1_000), 10, LocalDate.now());
            rafraichir(VUE);

            assertThat(services.topProductsReportService.getTopProductsByRevenue(LocalDate.now(), 2)).hasSize(2);
            assertThat(services.topProductsReportService.getAllProductsForMonth(LocalDate.now())).hasSize(3);
        }

        @Test
        @DisplayName("quantités, nombre de tickets et chiffre d'affaires suivent le produit")
        void indicateursDuProduit() {
            Produit produit = produitReference("DOLIPRANE", 10_000);
            vendu(produit, 3, LocalDate.now());
            vendu(produit, 7, LocalDate.now());
            rafraichir(VUE);

            TopProductDTO ligne = services.topProductsReportService.getAllProductsForMonth(LocalDate.now()).getFirst();

            assertThat(ligne.qteVendue()).isEqualTo(10);
            assertThat(ligne.nbVentes()).isEqualTo(2);
            assertThat(ligne.caGenere()).isEqualTo(100_000);
            assertThat(ligne.codeCip()).isNotBlank();
        }

        /** Le prix moyen est celui réellement pratiqué : c'est lui qui révèle les remises de fond. */
        @Test
        @DisplayName("le prix moyen rapporte le montant encaissé aux quantités")
        void prixMoyen() {
            vendu(produitReference("DOLIPRANE", 10_000), 4, LocalDate.now());
            rafraichir(VUE);

            assertThat(services.topProductsReportService.getAllProductsForMonth(LocalDate.now()).getFirst().prixMoyen())
                .isEqualByComparingTo("10000");
        }
    }

    // ===== découpage mensuel =====

    @Nested
    @DisplayName("Découpage mensuel")
    class DecoupageMensuel {

        /**
         * L'écran envoie une date quelconque — souvent « aujourd'hui ». Le service la ramène au
         * premier du mois, sans quoi un palmarès demandé le 18 ne trouverait jamais sa ligne.
         */
        @Test
        @DisplayName("n'importe quelle date du mois donne le palmarès de ce mois")
        void dateNormaliseeAuPremierDuMois() {
            LocalDate premierDuMois = LocalDate.now().withDayOfMonth(1);
            vendu(produitReference("DOLIPRANE", 10_000), 10, premierDuMois);
            rafraichir(VUE);

            assertThat(services.topProductsReportService.getAllProductsForMonth(premierDuMois)).hasSize(1);
            assertThat(services.topProductsReportService.getAllProductsForMonth(premierDuMois.plusDays(15)))
                .extracting(TopProductDTO::libelle)
                .containsExactly("DOLIPRANE");
        }

        @Test
        @DisplayName("le mois rendu est bien le premier jour du mois")
        void moisRendu() {
            LocalDate premierDuMois = LocalDate.now().withDayOfMonth(1);
            vendu(produitReference("DOLIPRANE", 10_000), 10, LocalDate.now());
            rafraichir(VUE);

            assertThat(services.topProductsReportService.getAllProductsForMonth(LocalDate.now()).getFirst().mois())
                .isEqualTo(premierDuMois);
        }

        @Test
        @DisplayName("les ventes d'un autre mois ne se mêlent pas au palmarès")
        void moisCloisonnes() {
            LocalDate moisPrecedent = LocalDate.now().withDayOfMonth(1).minusMonths(1);
            vendu(produitReference("CE MOIS", 10_000), 10, LocalDate.now().withDayOfMonth(1));
            vendu(produitReference("MOIS DERNIER", 10_000), 10, moisPrecedent);
            rafraichir(VUE);

            assertThat(services.topProductsReportService.getAllProductsForMonth(LocalDate.now()))
                .extracting(TopProductDTO::libelle)
                .containsExactly("CE MOIS");
            assertThat(services.topProductsReportService.getAllProductsForMonth(moisPrecedent))
                .extracting(TopProductDTO::libelle)
                .containsExactly("MOIS DERNIER");
        }

        @Test
        @DisplayName("l'évolution d'un produit rend un point par mois, du plus récent au plus ancien")
        void evolutionMensuelle() {
            Produit produit = produitReference("DOLIPRANE", 10_000);
            vendu(produit, 10, LocalDate.now().withDayOfMonth(1));
            vendu(produit, 20, LocalDate.now().withDayOfMonth(1).minusMonths(1));
            vendu(produit, 30, LocalDate.now().withDayOfMonth(1).minusMonths(2));
            rafraichir(VUE);

            List<TopProductDTO> evolution = services.topProductsReportService.getProductMonthlyEvolution(produit.getId(), 6);

            assertThat(evolution).extracting(TopProductDTO::qteVendue).containsExactly(10, 20, 30);
        }

        /** La vue ne conserve que six mois : demander davantage ne peut rien ramener de plus. */
        @Test
        @DisplayName("l'évolution est bornée à la profondeur de la vue")
        void evolutionBorneeASixMois() {
            Produit produit = produitReference("DOLIPRANE", 10_000);
            vendu(produit, 10, LocalDate.now().withDayOfMonth(1));
            rafraichir(VUE);

            assertThat(services.topProductsReportService.getProductMonthlyEvolution(produit.getId(), 24)).hasSize(1);
        }

        @Test
        @DisplayName("un produit sans vente n'a pas d'évolution")
        void evolutionSansVente() {
            Produit produit = produitReference("DORMANT", 10_000);
            rafraichir(VUE);

            assertThat(services.topProductsReportService.getProductMonthlyEvolution(produit.getId(), 6)).isEmpty();
        }
    }

    // ===== périmètre =====

    @Nested
    @DisplayName("Périmètre")
    class Perimetre {

        @Test
        @DisplayName("une vente annulée ne figure pas au palmarès")
        void venteAnnuleeExclue() {
            vendu(produitReference("DOLIPRANE", 10_000), 100, LocalDate.now(), true, CategorieChiffreAffaire.CA);
            rafraichir(VUE);

            assertThat(services.topProductsReportService.getAllProductsForMonth(LocalDate.now())).isEmpty();
        }

        @Test
        @DisplayName("une vente hors chiffre d'affaires ne figure pas au palmarès")
        void venteHorsChiffreDAffairesExclue() {
            vendu(produitReference("DOLIPRANE", 10_000), 100, LocalDate.now(), false, CategorieChiffreAffaire.CA_DEPOT);
            rafraichir(VUE);

            assertThat(services.topProductsReportService.getAllProductsForMonth(LocalDate.now())).isEmpty();
        }

        /** Le palmarès porte sur ce qui se vend, quel que soit le conditionnement. */
        @Test
        @DisplayName("les produits au détail y figurent aussi")
        void produitDetailInclus() {
            Produit detail = produit(unique("AU DETAIL"), TypeProduit.DETAIL, 10_000, 5);
            referencement(detail, fournisseur("LABOREX " + unique("")));
            vendu(detail, 10, LocalDate.now());
            rafraichir(VUE);

            assertThat(services.topProductsReportService.getAllProductsForMonth(LocalDate.now())).hasSize(1);
        }

        @Test
        @DisplayName("base vierge : aucun palmarès")
        void baseVierge() {
            rafraichir(VUE);

            assertThat(services.topProductsReportService.getAllProductsForMonth(LocalDate.now())).isEmpty();
            assertThat(services.topProductsReportService.getTopProductsByRevenue(LocalDate.now(), 10)).isEmpty();
        }
    }

    // ===== fabrique locale =====

    private Produit produitReference(String libelle, int prixVente) {
        Produit produit = produit(libelle, TypeProduit.PACKAGE, prixVente, 5);
        referencement(produit, fournisseur("LABOREX " + unique("")));
        return produit;
    }
}
