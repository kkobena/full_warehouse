package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.repository.DashboardCARepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Le tableau de bord du chiffre d'affaires ne lit presque jamais les tables : il lit trois vues
 * matérialisées, rafraîchies par une fonction stockée. Ce détour a un prix — l'écran ne montre que ce
 * que le dernier rafraîchissement a figé — et une conséquence : les colonnes que le code nomme
 * appartiennent aux vues, non aux entités, donc rien dans le projet Java ne les garantit.
 *
 * <p>C'est la situation où une vue redéfinie par une migration et un code resté en arrière ne se
 * détectent qu'en exécutant la requête. Les quatre indicateurs financiers, eux, interrogent bien les
 * tables, et chacun sur un statut écrit en clair dans la chaîne SQL.
 */
@DisplayName("DashboardCARepository — tableau de bord lu sur PostgreSQL")
class DashboardCARepositoryIntegrationTest extends AbstractReportIntegrationTest {

    private DashboardCARepository repository;

    @BeforeEach
    void cablerLeRepository() {
        repository = new DashboardCARepository(em);
    }

    // ===== vues matérialisées =====

    @Nested
    @DisplayName("Vues du chiffre d'affaires")
    class VuesDuChiffreAffaires {

        /**
         * Le rafraîchissement passe par une fonction stockée : elle doit exister et couvrir les trois
         * vues que les requêtes interrogent ensuite.
         */
        @Test
        @DisplayName("le rafraîchissement des vues s'exécute")
        void rafraichissementDesVues() {
            Produit produit = produitAnalysable("DOLIPRANE DASHBOARD", 1_000);
            vendu(produit, 10, hier());
            em.flush();

            repository.refreshViews();

            assertThat(repository.findDailySummary(hier(), LocalDate.now())).isNotNull();
        }

        @Test
        @DisplayName("la synthèse journalière porte la journée vendue")
        void syntheseJournaliere() {
            Produit produit = produitAnalysable("DOLIPRANE JOUR", 1_000);
            vendu(produit, 10, hier());
            rafraichirLesVues();

            Object[] jour = jourDe(hier());

            assertThat(toLong(jour[1])).isPositive();
            assertThat(toLong(jour[3])).isGreaterThanOrEqualTo(10_000L);
        }

        @Test
        @DisplayName("les journées sont rendues de la plus récente à la plus ancienne")
        void ordreDecroissant() {
            Produit produit = produitAnalysable("DOLIPRANE ORDRE", 1_000);
            vendu(produit, 10, hier());
            vendu(produit, 10, avantHier());
            rafraichirLesVues();

            List<Object[]> jours = repository.findDailySummary(avantHier(), LocalDate.now());

            assertThat(jours).extracting(row -> row[0].toString()).isSortedAccordingTo(java.util.Comparator.reverseOrder());
        }

        @Test
        @DisplayName("l'agrégat de période cumule les journées de la fenêtre")
        void agregatDePeriode() {
            Produit produit = produitAnalysable("DOLIPRANE PERIODE", 1_000);
            vendu(produit, 10, hier());
            vendu(produit, 10, avantHier());
            rafraichirLesVues();

            Object[] agregat = repository.getPeriodAggregation(avantHier(), LocalDate.now());

            assertThat(toLong(agregat[0])).isGreaterThanOrEqualTo(20_000L);
            assertThat(toLong(agregat[1])).isGreaterThanOrEqualTo(2L);
        }

        /**
         * Le panier d'une période est le rapport de ses totaux, jamais la moyenne de ses journées :
         * une journée d'une vente ne pèse pas ce que pèse une journée de quatre. La tuile affiche
         * le CA et le nombre de transactions à côté du panier ; qui divise les deux premiers doit
         * retrouver le troisième, sinon l'écran se contredit sous les yeux du pharmacien.
         */
        @Test
        @DisplayName("le panier d'une période est le CA divisé par les transactions, non la moyenne des journées")
        void panierPondereSurLaPeriode() {
            Produit produit = produitAnalysable("DOLIPRANE PANIER", 1_000);
            vendu(produit, 1, avantHier());
            vendu(produit, 40, hier());
            vendu(produit, 40, hier());
            vendu(produit, 40, hier());
            rafraichirLesVues();

            Object[] agregat = repository.getPeriodAggregation(avantHier(), LocalDate.now());

            BigDecimal ca = BigDecimal.valueOf(toLong(agregat[0]));
            BigDecimal transactions = BigDecimal.valueOf(toLong(agregat[1]));
            assertThat((BigDecimal) agregat[2]).isEqualByComparingTo(ca.divide(transactions, 2, RoundingMode.HALF_UP));
        }

        /**
         * Le contrôle précédent passerait encore si toutes les journées portaient le même nombre de
         * ventes. Celui-ci ancre la régression : avec une journée déséquilibrée face à une autre,
         * l'ancien {@code AVG(panier_moyen)} donnait une valeur franchement différente.
         */
        @Test
        @DisplayName("une journée creuse ne pèse pas autant qu'une journée chargée")
        void journeeCreuseNePesePasAutant() {
            Produit produit = produitAnalysable("DOLIPRANE POIDS", 1_000);
            vendu(produit, 1, avantHier());
            vendu(produit, 40, hier());
            vendu(produit, 40, hier());
            vendu(produit, 40, hier());
            rafraichirLesVues();

            List<Object[]> journees = repository.findDailySummary(avantHier(), LocalDate.now());
            BigDecimal moyenneDesJournees = journees
                .stream()
                .map(journee -> (BigDecimal) journee[4])
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(journees.size()), 2, RoundingMode.HALF_UP);

            Object[] agregat = repository.getPeriodAggregation(avantHier(), LocalDate.now());

            assertThat((BigDecimal) agregat[2]).isNotEqualByComparingTo(moyenneDesJournees);
        }

        /** Une fenêtre sans vente doit rendre des zéros, non des valeurs nulles. */
        @Test
        @DisplayName("une période sans vente rend des zéros plutôt que rien")
        void periodeSansVente() {
            LocalDate demain = LocalDate.now().plusDays(1);

            Object[] agregat = repository.getPeriodAggregation(demain, demain.plusDays(1));

            assertThat(toLong(agregat[0])).isZero();
            assertThat(toLong(agregat[1])).isZero();
            assertThat(agregat[2]).isNotNull();
            assertThat(agregat[3]).isNotNull();
        }

        @Test
        @DisplayName("la répartition par mode de règlement s'interroge sans erreur")
        void repartitionParMode() {
            rafraichirLesVues();

            assertThat(repository.findPaymentMethodDistribution(avantHier(), LocalDate.now())).isNotNull();
        }

        @Test
        @DisplayName("la répartition par famille porte la famille vendue")
        void repartitionParFamille() {
            Produit produit = produitAnalysable("DOLIPRANE FAMILLE", 1_000);
            vendu(produit, 10, hier());
            rafraichirLesVues();

            String famille = produit.getFamille().getLibelle();

            assertThat(repository.findProductFamilyDistribution(hier(), LocalDate.now()))
                .anyMatch(row -> famille.equals(row[1]));
        }

        @Test
        @DisplayName("l'évolution du panier moyen est rendue mois par mois, dans l'ordre")
        void evolutionDuPanier() {
            Produit produit = produitAnalysable("DOLIPRANE PANIER", 1_000);
            vendu(produit, 10, hier());
            rafraichirLesVues();

            assertThat(repository.findBasketEvolutionMonthly())
                .extracting(row -> toLong(row[0]) * 100 + toLong(row[1]))
                .isSorted();
        }
    }

    // ===== palmarès =====

    @Nested
    @DisplayName("Palmarès des produits")
    class PalmaresDesProduits {

        @Test
        @DisplayName("le produit vendu porte ses quantités et son chiffre")
        void produitVendu() {
            Produit produit = produitAnalysable("DOLIPRANE TOP", 1_000);
            vendu(produit, 12, hier());
            em.flush();

            Object[] ligne = produitDuPalmares(produit);

            assertThat(toLong(ligne[5])).isEqualTo(12L);
            assertThat(toLong(ligne[6])).isEqualTo(12_000L);
        }

        /**
         * La requête joint les lignes de vente : un produit vendu sur deux tickets ne doit pas
         * compter pour deux produits mais pour deux ventes d'un même produit.
         */
        @Test
        @DisplayName("un produit vendu sur deux tickets compte deux ventes, une seule ligne")
        void deuxTicketsUnProduit() {
            Produit produit = produitAnalysable("DOLIPRANE DEUX", 1_000);
            vendu(produit, 5, hier());
            vendu(produit, 7, hier());
            em.flush();

            List<Object[]> lignes = repository
                .findTopProducts(hier(), LocalDate.now(), 100)
                .stream()
                .filter(row -> toLong(row[1]) == produit.getId())
                .toList();

            assertThat(lignes).hasSize(1);
            assertThat(toLong(lignes.getFirst()[4])).isEqualTo(2L);
            assertThat(toLong(lignes.getFirst()[5])).isEqualTo(12L);
        }

        @Test
        @DisplayName("le palmarès s'arrête au nombre de produits demandé")
        void plafondDuPalmares() {
            for (int i = 0; i < 4; i++) {
                vendu(produitAnalysable("PRODUIT-" + i, 1_000), 10 * (i + 1), hier());
            }
            em.flush();

            assertThat(repository.findTopProducts(hier(), LocalDate.now(), 2)).hasSize(2);
        }

        @Test
        @DisplayName("une vente annulée ne place aucun produit au palmarès")
        void venteAnnulee() {
            Produit produit = produitAnalysable("DOLIPRANE ANNULE", 1_000);
            vendu(produit, 10, hier(), true, CategorieChiffreAffaire.CA);
            em.flush();

            assertThat(repository.findTopProducts(hier(), LocalDate.now(), 100))
                .noneMatch(row -> toLong(row[1]) == produit.getId());
        }

        private Object[] produitDuPalmares(Produit produit) {
            return repository
                .findTopProducts(hier(), LocalDate.now(), 100)
                .stream()
                .filter(row -> toLong(row[1]) == produit.getId())
                .findFirst()
                .orElseThrow();
        }
    }

    // ===== remises =====

    @Nested
    @DisplayName("Remises")
    class Remises {

        @Test
        @DisplayName("les indicateurs de remise s'interrogent et rendent des valeurs")
        void indicateursDeRemise() {
            Produit produit = produitAnalysable("DOLIPRANE REMISE", 1_000);
            vendu(produit, 10, hier());
            em.flush();

            Object[] kpi = repository.findRemisesKpi(hier(), LocalDate.now());

            assertThat(kpi[0]).isNotNull();
            assertThat(toLong(kpi[1])).isGreaterThanOrEqualTo(10_000L);
            assertThat(toLong(kpi[4])).isPositive();
        }

        @Test
        @DisplayName("les produits les plus remisés s'interrogent sans erreur")
        void produitsLesPlusRemises() {
            assertThat(repository.findRemisesTopProducts(hier(), LocalDate.now(), 5)).isNotNull();
        }
    }

    // ===== équipe et génériques =====

    @Nested
    @DisplayName("Équipe et génériques")
    class EquipeEtGeneriques {

        @Test
        @DisplayName("la performance par vendeur rend une ligne par vendeur ayant vendu")
        void performanceParVendeur() {
            Produit produit = produitAnalysable("DOLIPRANE VENDEUR", 1_000);
            vendu(produit, 10, hier());
            em.flush();

            assertThat(repository.findSalesByStaff(hier(), LocalDate.now()))
                .anyMatch(row -> toLong(row[0]) == utilisateur.getId());
        }

        @Test
        @DisplayName("la substitution générique rend ses six compteurs")
        void substitutionGenerique() {
            Produit produit = produitAnalysable("DOLIPRANE GENERIQUE", 1_000);
            vendu(produit, 10, hier());
            em.flush();

            Object[] stats = repository.findGenericsSubstitutionStats(hier(), LocalDate.now());

            assertThat(stats).hasSize(6);
            assertThat(stats).allSatisfy(valeur -> assertThat(valeur).isNotNull());
        }
    }

    // ===== indicateurs financiers =====

    @Nested
    @DisplayName("Indicateurs financiers")
    class IndicateursFinanciers {

        @Test
        @DisplayName("une commande clôturée non réglée alourdit la dette fournisseur")
        void detteFournisseur() {
            long avant = repository.getDetteFournisseur();

            commandeCloturee(4_000_000);
            em.flush();

            assertThat(repository.getDetteFournisseur() - avant).isEqualTo(4_000_000L);
        }

        @Test
        @DisplayName("le reste dû des dossiers de tiers payant forme la créance")
        void creanceTiersPayant() {
            long avant = repository.getCreanceTiersPayant();

            facture(
                tiersPayant("CNAM", groupeTiersPayant("GROUPE-CNAM")),
                hier(),
                5_000_000,
                2_000_000,
                com.kobe.warehouse.domain.enumeration.InvoiceStatut.PARTIALLY_PAID
            );
            em.flush();

            assertThat(repository.getCreanceTiersPayant() - avant).isEqualTo(3_000_000L);
        }

        /** L'échéance se calcule sur les jours de crédit du fournisseur, à défaut trente. */
        @Test
        @DisplayName("une commande dépassant les jours de crédit du fournisseur est en retard")
        void echeanceDepassee() {
            long avant = repository.getNbEcheancesEnRetard();

            Commande commande = commandeCloturee(4_000_000);
            commande.setReceiptDate(jourDeLAnnee(120));
            em.flush();

            assertThat(repository.getNbEcheancesEnRetard() - avant).isEqualTo(1L);
        }

        @Test
        @DisplayName("une commande récente n'est pas encore échue")
        void echeanceNonDepassee() {
            long avant = repository.getNbEcheancesEnRetard();

            commandeCloturee(4_000_000);
            em.flush();

            assertThat(repository.getNbEcheancesEnRetard() - avant).isZero();
        }

        @Test
        @DisplayName("le nombre de ventes impayées s'interroge sans erreur")
        void ventesImpayees() {
            assertThat(repository.getNbFacturesImpayees()).isNotNegative();
        }
    }

    // ===== utilitaires =====

    private void rafraichirLesVues() {
        em.flush();
        rafraichir("mv_dashboard_ca_daily");
        rafraichir("mv_dashboard_ca_payment_methods");
        rafraichir("mv_dashboard_ca_product_families");
    }

    private Object[] jourDe(LocalDate jour) {
        return repository
            .findDailySummary(jour, jour)
            .stream()
            .findFirst()
            .orElseThrow();
    }

    private Commande commandeCloturee(int montant) {
        Commande commande = reception(fournisseur(unique("LABOREX")), hier(), hier(), montant, 10, 10);
        commande.setOrderStatus(OrderStatut.CLOSED);
        em.flush();
        return commande;
    }

    /** {@code sales} est partitionnée par date : on reste dans l'année en cours. */
    private static LocalDate hier() {
        return jourDeLAnnee(1);
    }

    private static LocalDate avantHier() {
        return jourDeLAnnee(2);
    }

    private static LocalDate jourDeLAnnee(int anciennete) {
        LocalDate jour = LocalDate.now().minusDays(anciennete);
        LocalDate premierJanvier = LocalDate.now().withDayOfYear(1);
        return jour.isBefore(premierJanvier) ? premierJanvier : jour;
    }

    private static long toLong(Object valeur) {
        return valeur != null ? ((Number) valeur).longValue() : 0L;
    }
}
