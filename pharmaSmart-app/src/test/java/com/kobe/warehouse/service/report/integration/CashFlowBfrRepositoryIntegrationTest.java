package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.repository.CashFlowBfrRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Le besoin en fonds de roulement se lit sur six agrégats indépendants, chacun tiré d'une table
 * différente : le stock d'une vue matérialisée, les créances des dossiers de tiers payant, la dette
 * des commandes fournisseur moins ce qui leur a déjà été réglé, et trois flux annuels qui servent de
 * dénominateurs aux délais de rotation.
 *
 * <p>Six requêtes qui n'ont rien en commun, écrites en SQL natif contre des tables et une vue que le
 * compilateur ne regarde pas. C'est le cas de figure où une colonne renommée ou un statut mal
 * orthographié ne se découvre qu'à l'exécution — et où le simple fait que l'écran s'ouvre est déjà
 * une information.
 */
@DisplayName("CashFlowBfrRepository — besoin en fonds de roulement lu sur PostgreSQL")
class CashFlowBfrRepositoryIntegrationTest extends AbstractReportIntegrationTest {

    private CashFlowBfrRepository repository;

    @BeforeEach
    void cablerLeRepository() {
        repository = new CashFlowBfrRepository(em);
    }

    // ===== stock =====

    @Nested
    @DisplayName("Valeur du stock")
    class ValeurDuStock {

        /**
         * La valeur du stock se lit sur une vue matérialisée, par magasin. Interroger un magasin qui
         * n'a pas de stock ne doit pas rendre {@code null} mais zéro.
         */
        @Test
        @DisplayName("le stock est valorisé au prix d'achat du magasin demandé")
        void stockValorise() {
            long avant = repository.getStockValue(MAGASIN_ID);

            Produit produit = produit("DOLIPRANE BFR", TypeProduit.PACKAGE, 10_000, 1);
            referencement(produit, fournisseur("LABOREX BFR"));
            stock(produit, 12);
            rafraichir("mv_stock_valuation");

            // Le référencement porte un prix d'achat de six mille : douze unités valent 72 000.
            assertThat(repository.getStockValue(MAGASIN_ID) - avant).isEqualTo(72_000L);
        }

        @Test
        @DisplayName("un magasin sans stock vaut zéro plutôt que rien")
        void magasinSansStock() {
            assertThat(repository.getStockValue(999_999)).isZero();
        }
    }

    // ===== créances =====

    @Nested
    @DisplayName("Créances tiers payant")
    class CreancesTiersPayant {

        @Test
        @DisplayName("le reste dû des dossiers de tiers payant entre en créance")
        void resteDuDesDossiers() {
            long avant = repository.getCreanceTp();

            facture(payeur("CNAM"), hier(), 5_000_000, 2_000_000, InvoiceStatut.PARTIALLY_PAID);
            em.flush();

            assertThat(repository.getCreanceTp() - avant).isEqualTo(3_000_000L);
        }

        @Test
        @DisplayName("un dossier entièrement réglé ne laisse aucune créance")
        void dossierRegle() {
            long avant = repository.getCreanceTp();

            facture(payeur("CNAM"), hier(), 5_000_000, 5_000_000, InvoiceStatut.PAID);
            em.flush();

            assertThat(repository.getCreanceTp() - avant).isZero();
        }
    }

    // ===== dettes =====

    @Nested
    @DisplayName("Dettes fournisseurs")
    class DettesFournisseurs {

        @Test
        @DisplayName("une commande clôturée non réglée constitue une dette")
        void commandeNonReglee() {
            long avant = repository.getDetteFournisseur();

            commandeCloturee(4_000_000);
            em.flush();

            assertThat(repository.getDetteFournisseur() - avant).isEqualTo(4_000_000L);
        }

        /** Une commande encore en réception n'est pas une dette : elle n'est pas due. */
        @Test
        @DisplayName("une commande non clôturée n'est pas une dette")
        void commandeNonCloturee() {
            long avant = repository.getDetteFournisseur();

            reception(fournisseur("LABOREX"), hier(), hier(), 4_000_000, 10, 10);
            em.flush();

            assertThat(repository.getDetteFournisseur() - avant).isZero();
        }

        @Test
        @DisplayName("une commande hors des douze derniers mois sort du périmètre")
        void commandeAncienne() {
            long avant = repository.getDetteFournisseur();

            Commande commande = commandeCloturee(4_000_000);
            commande.setReceiptDate(LocalDate.now().minusYears(2));
            em.flush();

            assertThat(repository.getDetteFournisseur() - avant).isZero();
        }
    }

    // ===== flux annuels =====

    @Nested
    @DisplayName("Flux des douze derniers mois")
    class FluxAnnuels {

        @Test
        @DisplayName("le coût d'achat écoulé suit les lignes de vente")
        void coutDesVentes() {
            long avant = repository.getCogs12m();

            Produit produit = produitAnalysable("DOLIPRANE COGS", 1_000);
            vendu(produit, 10, hier());
            em.flush();

            // Coût unitaire six cents, dix unités : six mille de marchandise écoulée.
            assertThat(repository.getCogs12m() - avant).isEqualTo(6_000L);
        }

        @Test
        @DisplayName("une vente annulée ne consomme pas de marchandise")
        void venteAnnulee() {
            long avant = repository.getCogs12m();

            Produit produit = produitAnalysable("DOLIPRANE ANNULE", 1_000);
            vendu(produit, 10, hier(), true, CategorieChiffreAffaire.CA);
            em.flush();

            assertThat(repository.getCogs12m() - avant).isZero();
        }

        @Test
        @DisplayName("le facturé tiers payant de l'année sert de référence aux créances")
        void factureTiersPayantAnnuel() {
            long avant = repository.getCaTp12m();

            facture(payeur("CNAM"), hier(), 5_000_000, 0, InvoiceStatut.NOT_PAID);
            em.flush();

            assertThat(repository.getCaTp12m() - avant).isEqualTo(5_000_000L);
        }

        @Test
        @DisplayName("les achats de l'année servent de référence aux dettes")
        void achatsAnnuels() {
            long avant = repository.getAchats12m();

            commandeCloturee(4_000_000);
            em.flush();

            assertThat(repository.getAchats12m() - avant).isEqualTo(4_000_000L);
        }

        /** Une officine au repos doit rendre zéro, non {@code null} : les délais divisent par ces flux. */
        @Test
        @DisplayName("aucun flux ne rend jamais null")
        void aucunFlux() {
            assertThat(repository.getCogs12m()).isNotNegative();
            assertThat(repository.getCaTp12m()).isNotNegative();
            assertThat(repository.getAchats12m()).isNotNegative();
        }
    }

    // ===== évolution =====

    @Nested
    @DisplayName("Évolution sur douze mois")
    class Evolution {

        /** La série est bâtie sur une table de mois : elle compte douze points, alimentés ou non. */
        @Test
        @DisplayName("la série couvre douze mois, y compris ceux sans mouvement")
        void douzeMois() {
            assertThat(repository.findEvolution()).hasSize(12);
        }

        @Test
        @DisplayName("les mois sont rendus dans l'ordre chronologique")
        void ordreChronologique() {
            assertThat(repository.findEvolution()).extracting(row -> toLong(row[0]) * 100 + toLong(row[1])).isSorted();
        }

        @Test
        @DisplayName("le mois en cours porte les créances émises et les achats reçus")
        void moisEnCoursAlimente() {
            Object[] avant = moisEnCours();
            long creancesAvant = toLong(avant[2]);
            long achatsAvant = toLong(avant[3]);

            facture(payeur("CNAM"), LocalDate.now().withDayOfMonth(1).plusDays(1), 5_000_000, 0, InvoiceStatut.NOT_PAID);
            Commande commande = commandeCloturee(4_000_000);
            commande.setReceiptDate(LocalDate.now().withDayOfMonth(1).plusDays(1));
            em.flush();

            Object[] apres = moisEnCours();

            assertThat(toLong(apres[2]) - creancesAvant).isEqualTo(5_000_000L);
            assertThat(toLong(apres[3]) - achatsAvant).isEqualTo(4_000_000L);
        }

        private Object[] moisEnCours() {
            return repository
                .findEvolution()
                .stream()
                .filter(row -> toLong(row[0]) == LocalDate.now().getYear() && toLong(row[1]) == LocalDate.now().getMonthValue())
                .findFirst()
                .orElseThrow();
        }
    }

    // ===== l'écran s'ouvre =====

    @Nested
    @DisplayName("Cohérence d'ensemble")
    class CoherenceDEnsemble {

        /**
         * Les six agrégats sont lus à la suite pour composer l'écran. Qu'ils s'exécutent tous sur une
         * base réelle est le minimum : une colonne renommée dans l'un d'eux suffit à faire échouer
         * l'ouverture du tableau, sans que rien ne l'ait signalé à la compilation.
         */
        @Test
        @DisplayName("les six agrégats s'exécutent et composent un besoin cohérent")
        void sixAgregats() {
            Produit produit = produitAnalysable("DOLIPRANE BFR", 1_000);
            vendu(produit, 10, hier());
            facture(payeur("CNAM"), hier(), 5_000_000, 1_000_000, InvoiceStatut.PARTIALLY_PAID);
            commandeCloturee(3_000_000);
            rafraichir("mv_stock_valuation");

            long stock = repository.getStockValue(MAGASIN_ID);
            long creances = repository.getCreanceTp();
            long dettes = repository.getDetteFournisseur();

            assertThat(stock).isNotNegative();
            assertThat(creances).isGreaterThanOrEqualTo(4_000_000L);
            assertThat(dettes).isGreaterThanOrEqualTo(3_000_000L);
            assertThat(repository.findEvolution()).isNotEmpty();
        }
    }

    // ===== utilitaires =====

    private Commande commandeCloturee(int montant) {
        Fournisseur fournisseur = fournisseur(unique("LABOREX"));
        Commande commande = reception(fournisseur, hier(), hier(), montant, 10, 10);
        commande.setOrderStatus(OrderStatut.CLOSED);
        em.flush();
        return commande;
    }

    private TiersPayant payeur(String nom) {
        return tiersPayant(nom, groupeTiersPayant("GROUPE-" + nom));
    }

    /** {@code sales} est partitionnée par date : on reste dans l'année en cours. */
    private static LocalDate hier() {
        LocalDate hier = LocalDate.now().minusDays(1);
        LocalDate premierJanvier = LocalDate.now().withDayOfYear(1);
        return hier.isBefore(premierJanvier) ? premierJanvier : hier;
    }

    private static long toLong(Object valeur) {
        return valeur != null ? ((Number) valeur).longValue() : 0L;
    }
}
