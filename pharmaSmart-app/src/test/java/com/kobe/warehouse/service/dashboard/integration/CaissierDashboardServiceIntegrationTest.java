package com.kobe.warehouse.service.dashboard.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.CashRegisterStatut;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.service.dto.dashboard.CaissierDashboardDTO;
import com.kobe.warehouse.service.dto.dashboard.CaisseStatusDTO;
import com.kobe.warehouse.service.dto.dashboard.DiffereARelancerDTO;
import com.kobe.warehouse.service.dto.dashboard.EncaissementParModeDTO;
import com.kobe.warehouse.service.dto.dashboard.LivraisonAttendueDTO;
import com.kobe.warehouse.service.dto.dashboard.SessionEncaissementsDTO;
import com.kobe.warehouse.service.dto.dashboard.VenteRecenteDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Le tableau de bord du préparateur repose entièrement sur des requêtes natives : sept requêtes qui
 * nomment leurs colonnes à la main, joignent des tables partitionnées et comparent des dates à
 * {@code CURRENT_DATE}. Rien de tout cela n'est vérifié à la compilation — c'est ici, contre un
 * vrai PostgreSQL migré par Flyway, qu'on l'éprouve.
 *
 * <p>Trois propriétés valent d'être tenues à ce niveau. Le <b>cloisonnement par caissier</b>
 * d'abord : la caisse du voisin ne doit apparaître dans aucune tuile, et c'est la jointure sur
 * {@code app_user.login} qui le garantit, pas le code Java. Les <b>exclusions</b> ensuite — un
 * règlement fournisseur sort de la caisse, l'y compter comme une recette gonflerait les espèces
 * théoriques et ferait annoncer un excédent au comptage. Enfin les <b>fenêtres de temps</b> : une
 * vente d'hier ou une commande d'un autre jour n'ont rien à faire dans un écran qui dit « le
 * jour ».
 */
@DisplayName("CaissierDashboardService — requêtes natives sur une vraie base")
class CaissierDashboardServiceIntegrationTest extends AbstractDashboardIntegrationTest {

    // ===== état de la caisse =====

    @Nested
    @DisplayName("État de la caisse")
    class EtatDeLaCaisse {

        @Test
        @DisplayName("additionne le fonds d'ouverture et les espèces encaissées de la session")
        void especesTheoriques() {
            CashRegister caisse = caisseOuverte(50_000L);
            encaissement(caisse, "CASH", TypeFinancialTransaction.CASH_SALE, 120_000);
            encaissement(caisse, "CASH", TypeFinancialTransaction.REGLEMENT_DIFFERE, 30_000);
            viderLeCache();

            CaisseStatusDTO resultat = services.caissierDashboardService.getCaisseStatus();

            assertThat(resultat.fondOuverture()).isEqualTo(50_000L);
            assertThat(resultat.encaissementsEspeces()).isEqualTo(150_000L);
            assertThat(resultat.especesTheoriques()).isEqualTo(200_000L);
            assertThat(resultat.etat()).isEqualTo("OUVERTE");
        }

        /**
         * Le filtre {@code payment_group = 'CASH'} est le cœur de la tuile : un paiement mobile ne
         * remplit pas le tiroir, et le faire compter comme des espèces ferait chercher au
         * préparateur un manquant qui n'existe pas.
         */
        @Test
        @DisplayName("ignore les encaissements qui ne sont pas en espèces")
        void ignoreLesModesNonEspeces() {
            CashRegister caisse = caisseOuverte(0L);
            encaissement(caisse, "CASH", TypeFinancialTransaction.CASH_SALE, 10_000);
            encaissement(caisse, "OM", TypeFinancialTransaction.CASH_SALE, 90_000);
            encaissement(caisse, "CB", TypeFinancialTransaction.CASH_SALE, 70_000);
            viderLeCache();

            assertThat(services.caissierDashboardService.getCaisseStatus().encaissementsEspeces()).isEqualTo(10_000L);
        }

        /**
         * Une sortie de caisse n'est pas une recette. Elle est exclue par la liste des types de
         * transaction retenus : sans cette exclusion, l'écran additionnerait ce qui est sorti.
         */
        @Test
        @DisplayName("ignore les mouvements qui ne sont pas des recettes")
        void ignoreLesSorties() {
            CashRegister caisse = caisseOuverte(0L);
            encaissement(caisse, "CASH", TypeFinancialTransaction.CASH_SALE, 80_000);
            encaissement(caisse, "CASH", TypeFinancialTransaction.SORTIE_CAISSE, 25_000);
            encaissement(caisse, "CASH", TypeFinancialTransaction.REGLMENT_FOURNISSEUR, 40_000);
            viderLeCache();

            assertThat(services.caissierDashboardService.getCaisseStatus().encaissementsEspeces()).isEqualTo(80_000L);
        }

        @Test
        @DisplayName("l'heure d'ouverture est celle de la session, au format HH:mm")
        void heureDOuverture() {
            caisse(0L, aujourdHui().atTime(8, 7), CashRegisterStatut.OPEN, null);
            viderLeCache();

            assertThat(services.caissierDashboardService.getCaisseStatus().heureOuverture()).isEqualTo("08:07");
        }

        @Test
        @DisplayName("caisse fermée dans la journée : l'état bascule et l'heure de fermeture apparaît")
        void caisseFermee() {
            LocalDateTime fermeture = aujourdHui().atTime(19, 30);
            caisse(20_000L, aujourdHui().atTime(8, 0), CashRegisterStatut.CLOSED, fermeture);
            viderLeCache();

            CaisseStatusDTO resultat = services.caissierDashboardService.getCaisseStatus();

            assertThat(resultat.etat()).isEqualTo("FERMEE");
            assertThat(resultat.derniereFermeture()).isEqualTo(fermeture);
        }

        /**
         * Pas de caisse ouverte aujourd'hui : l'écran retombe sur la dernière session close, qui
         * est la seule chose qu'il puisse encore dire d'utile au préparateur.
         */
        @Test
        @DisplayName("aucune caisse aujourd'hui : la dernière fermeture connue est remontée")
        void aucuneCaisseAujourdhui() {
            LocalDateTime hier = aujourdHui().minusDays(1).atTime(19, 0);
            caisse(0L, hier.minusHours(11), CashRegisterStatut.VALIDATED, hier);
            viderLeCache();

            CaisseStatusDTO resultat = services.caissierDashboardService.getCaisseStatus();

            assertThat(resultat.etat()).isEqualTo("FERMEE");
            assertThat(resultat.especesTheoriques()).isZero();
            assertThat(resultat.derniereFermeture()).isEqualTo(hier);
        }

        @Test
        @DisplayName("base vierge : tout est à zéro, sans erreur")
        void baseVierge() {
            CaisseStatusDTO resultat = services.caissierDashboardService.getCaisseStatus();

            assertThat(resultat.etat()).isEqualTo("FERMEE");
            assertThat(resultat.especesTheoriques()).isZero();
            assertThat(resultat.derniereFermeture()).isNull();
        }

        /**
         * Le cloisonnement se joue dans la jointure {@code app_user.login} : c'est elle, et non le
         * code Java, qui empêche un préparateur de voir la caisse d'un collègue.
         */
        @Test
        @DisplayName("la caisse d'un autre utilisateur reste invisible")
        void caisseDUnAutreUtilisateurInvisible() {
            CashRegister caisse = caisseOuverte(50_000L);
            encaissement(caisse, "CASH", TypeFinancialTransaction.CASH_SALE, 300_000);
            viderLeCache();

            SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("admin", null, List.of()));

            CaisseStatusDTO resultat = services.caissierDashboardService.getCaisseStatus();

            assertThat(resultat.especesTheoriques()).isZero();
            assertThat(resultat.etat()).isEqualTo("FERMEE");
        }
    }

    // ===== encaissements de la session =====

    @Nested
    @DisplayName("Encaissements de la session")
    class EncaissementsDeLaSession {

        @Test
        @DisplayName("une ligne par mode utilisé, dans l'ordre d'affichage des modes de paiement")
        void uneLigneParModeDansLOrdreDeTri() {
            CashRegister caisse = caisseOuverte(0L);
            encaissement(caisse, "CB", TypeFinancialTransaction.CASH_SALE, 70_000);
            encaissement(caisse, "CASH", TypeFinancialTransaction.CASH_SALE, 100_000);
            encaissement(caisse, "OM", TypeFinancialTransaction.CASH_SALE, 40_000);
            viderLeCache();

            SessionEncaissementsDTO resultat = services.caissierDashboardService.getSessionEncaissements();

            // ordre_tri : CASH (1), OM (2), CB (6)
            assertThat(resultat.lignes()).extracting(EncaissementParModeDTO::code).containsExactly("CASH", "OM", "CB");
            assertThat(resultat.totalEncaisse()).isEqualTo(210_000L);
        }

        @Test
        @DisplayName("les encaissements d'un même mode sont agrégés en une seule ligne")
        void agregationParMode() {
            CashRegister caisse = caisseOuverte(0L);
            encaissement(caisse, "CASH", TypeFinancialTransaction.CASH_SALE, 10_000);
            encaissement(caisse, "CASH", TypeFinancialTransaction.CREDIT_SALE, 15_000);
            encaissement(caisse, "CASH", TypeFinancialTransaction.ENTREE_CAISSE, 5_000);
            viderLeCache();

            SessionEncaissementsDTO resultat = services.caissierDashboardService.getSessionEncaissements();

            assertThat(resultat.lignes()).hasSize(1);
            assertThat(resultat.lignes().getFirst().montant()).isEqualTo(30_000L);
        }

        @Test
        @DisplayName("carnet et différé viennent des ventes, pas des règlements")
        void carnetEtDiffereViennentDesVentes() {
            CashRegister caisse = caisseOuverte(0L);
            encaissement(caisse, "CASH", TypeFinancialTransaction.CASH_SALE, 100_000);
            venteComptant(caisse, 100_000);
            venteCarnet(caisse, 35_000);
            venteDifferee(caisse, 60_000, 20_000, client("Kouassi", "Yao", "0102030405"), aujourdHui());
            viderLeCache();

            SessionEncaissementsDTO resultat = services.caissierDashboardService.getSessionEncaissements();

            assertThat(resultat.carnet()).isEqualTo(35_000L);
            assertThat(resultat.differe()).isEqualTo(20_000L);
            assertThat(resultat.totalEncaisse()).isEqualTo(100_000L);
            assertThat(resultat.totalARecouvrer()).isEqualTo(55_000L);
            assertThat(resultat.nombreTransactions()).isEqualTo(3);
        }

        @Test
        @DisplayName("aucune caisse ouverte : la session est vide")
        void aucuneCaisse() {
            SessionEncaissementsDTO resultat = services.caissierDashboardService.getSessionEncaissements();

            assertThat(resultat.lignes()).isEmpty();
            assertThat(resultat.nombreTransactions()).isZero();
        }

        /**
         * Un règlement fournisseur porte le {@code dtype} {@code PaymentFournisseur} : il sort de la
         * caisse au lieu d'y entrer, et la requête l'écarte explicitement.
         */
        @Test
        @DisplayName("une caisse sans encaissement rend une session à zéro, pas une ligne fantôme")
        void caisseSansEncaissement() {
            caisseOuverte(50_000L);
            viderLeCache();

            SessionEncaissementsDTO resultat = services.caissierDashboardService.getSessionEncaissements();

            assertThat(resultat.lignes()).isEmpty();
            assertThat(resultat.totalEncaisse()).isZero();
            assertThat(resultat.totalARecouvrer()).isZero();
        }
    }

    // ===== différés à relancer =====

    @Nested
    @DisplayName("Différés à relancer")
    class DifferesARelancer {

        @Test
        @DisplayName("classe les différés du plus ancien au plus récent, avec leur urgence")
        void classementParAnciennete() {
            CashRegister caisse = caisseOuverte(0L);
            UninsuredCustomer client = client("Kouassi", "Yao", "0102030405");
            venteDifferee(caisse, 10_000, 10_000, client, aujourdHui());
            venteDifferee(caisse, 20_000, 20_000, client, aujourdHui().minusDays(3));
            venteDifferee(caisse, 30_000, 30_000, client, aujourdHui().minusDays(15));
            viderLeCache();

            List<DiffereARelancerDTO> differes = services.caissierDashboardService.getDifferesRelance().differes();

            assertThat(differes).extracting(DiffereARelancerDTO::urgence).containsExactly("CRITIQUE", "RETARD", "AUJOURD_HUI");
            assertThat(differes).extracting(DiffereARelancerDTO::joursRetard).containsExactly(15, 3, 0);
        }

        @Test
        @DisplayName("reporte le nom et le téléphone du client, dont dépend la relance")
        void reporteLeContactDuClient() {
            CashRegister caisse = caisseOuverte(0L);
            venteDifferee(caisse, 12_500, 12_500, client("Kone", "Ali", "0700112233"), aujourdHui());
            viderLeCache();

            DiffereARelancerDTO differe = services.caissierDashboardService.getDifferesRelance().differes().getFirst();

            assertThat(differe.clientNom()).isEqualTo("Ali Kone");
            assertThat(differe.clientTelephone()).isEqualTo("0700112233");
            assertThat(differe.montantDu()).isEqualTo(12_500L);
        }

        @Test
        @DisplayName("un différé sans client se présente comme anonyme")
        void differeSansClient() {
            CashRegister caisse = caisseOuverte(0L);
            venteDifferee(caisse, 5_000, 5_000, null, aujourdHui());
            viderLeCache();

            DiffereARelancerDTO differe = services.caissierDashboardService.getDifferesRelance().differes().getFirst();

            assertThat(differe.clientNom()).isEqualTo("Anonyme");
            assertThat(differe.clientTelephone()).isNull();
        }

        /**
         * Un différé soldé n'est plus à relancer. Le filtre {@code rest_to_pay > 0} est ce qui
         * évite de rappeler un client qui a déjà payé.
         */
        @Test
        @DisplayName("un différé soldé disparaît de la liste")
        void differeSoldeDisparait() {
            CashRegister caisse = caisseOuverte(0L);
            UninsuredCustomer client = client("Kouassi", "Yao", "0102030405");
            venteDifferee(caisse, 10_000, 0, client, aujourdHui());
            venteDifferee(caisse, 20_000, 20_000, client, aujourdHui());
            viderLeCache();

            assertThat(services.caissierDashboardService.getDifferesRelance().differes()).hasSize(1);
        }

        @Test
        @DisplayName("une vente comptant impayée n'est pas un différé")
        void venteComptantImpayeeNEstPasUnDiffere() {
            CashRegister caisse = caisseOuverte(0L);
            vente(caisse, NatureVente.COMPTANT, 10_000, 10_000, false, null, aujourdHui());
            viderLeCache();

            assertThat(services.caissierDashboardService.getDifferesRelance().differes()).isEmpty();
        }

        @Test
        @DisplayName("le résumé compte les échéances du jour et totalise le dû")
        void resume() {
            CashRegister caisse = caisseOuverte(0L);
            UninsuredCustomer client = client("Kouassi", "Yao", "0102030405");
            venteDifferee(caisse, 10_000, 10_000, client, aujourdHui());
            venteDifferee(caisse, 20_000, 20_000, client, aujourdHui());
            venteDifferee(caisse, 30_000, 30_000, client, aujourdHui().minusDays(5));
            viderLeCache();

            var resume = services.caissierDashboardService.getDifferesRelance();

            assertThat(resume.nombreEcheancesAujourdhui()).isEqualTo(2);
            assertThat(resume.montantTotalDu()).isEqualTo(60_000L);
        }
    }

    // ===== livraisons attendues =====

    @Nested
    @DisplayName("Livraisons attendues")
    class LivraisonsAttendues {

        @Test
        @DisplayName("compte les références de chaque commande attendue chez son fournisseur")
        void compteLesReferences() {
            Fournisseur laborex = fournisseur("LABOREX " + unique(""));
            commande(laborex, OrderStatut.REQUESTED, 3);
            viderLeCache();

            List<LivraisonAttendueDTO> livraisons = services.caissierDashboardService.getLivraisonsJour();

            assertThat(livraisons).hasSize(1);
            assertThat(livraisons.getFirst().fournisseurNom()).startsWith("LABOREX");
            assertThat(livraisons.getFirst().nombreReferences()).isEqualTo(3);
        }

        @Test
        @DisplayName("les commandes attendues et celles en cours de réception sont toutes deux listées")
        void statutsRetenus() {
            commande(fournisseur("A " + unique("")), OrderStatut.REQUESTED, 1);
            commande(fournisseur("B " + unique("")), OrderStatut.RECEIVED, 2);
            viderLeCache();

            assertThat(services.caissierDashboardService.getLivraisonsJour()).hasSize(2);
        }

        @Test
        @DisplayName("une commande clôturée n'est plus attendue")
        void commandeClotureeExclue() {
            commande(fournisseur("A " + unique("")), OrderStatut.CLOSED, 1);
            viderLeCache();

            assertThat(services.caissierDashboardService.getLivraisonsJour()).isEmpty();
        }

        @Test
        @DisplayName("une commande sans ligne compte zéro référence plutôt que de disparaître")
        void commandeSansLigne() {
            commande(fournisseur("A " + unique("")), OrderStatut.REQUESTED, 0);
            viderLeCache();

            assertThat(services.caissierDashboardService.getLivraisonsJour()).hasSize(1);
            assertThat(services.caissierDashboardService.getLivraisonsJour().getFirst().nombreReferences()).isZero();
        }
    }

    // ===== ventes récentes =====

    @Nested
    @DisplayName("Ventes récentes")
    class VentesRecentes {

        @Test
        @DisplayName("les ventes du jour remontent, la plus récente en tête")
        void ordreAntichronologique() {
            CashRegister caisse = caisseOuverte(0L);
            venteComptant(caisse, 10_000);
            venteComptant(caisse, 20_000);
            venteComptant(caisse, 30_000);
            viderLeCache();

            List<VenteRecenteDTO> ventes = services.caissierDashboardService.getVentesRecentes(8);

            assertThat(ventes).extracting(VenteRecenteDTO::montant).containsExactly(30_000L, 20_000L, 10_000L);
        }

        @Test
        @DisplayName("une vente d'hier n'est pas une vente récente")
        void venteDHierExclue() {
            CashRegister caisse = caisseOuverte(0L);
            vente(caisse, NatureVente.COMPTANT, 10_000, 0, false, null, aujourdHui().minusDays(1));
            venteComptant(caisse, 20_000);
            viderLeCache();

            assertThat(services.caissierDashboardService.getVentesRecentes(8))
                .extracting(VenteRecenteDTO::montant)
                .containsExactly(20_000L);
        }

        /** Un différé s'affiche comme tel, quelle que soit la nature de la vente qui l'a produit. */
        @Test
        @DisplayName("une vente différée est typée DIFFERE")
        void venteDiffereeTypee() {
            CashRegister caisse = caisseOuverte(0L);
            venteDifferee(caisse, 10_000, 10_000, client("Kouassi", "Yao", "0102030405"), aujourdHui());
            viderLeCache();

            VenteRecenteDTO vente = services.caissierDashboardService.getVentesRecentes(8).getFirst();

            assertThat(vente.typeVente()).isEqualTo("DIFFERE");
            assertThat(vente.clientNom()).isEqualTo("Yao Kouassi");
        }

        @Test
        @DisplayName("la limite demandée est appliquée après la requête")
        void limiteAppliquee() {
            CashRegister caisse = caisseOuverte(0L);
            for (int i = 1; i <= 5; i++) {
                venteComptant(caisse, 1_000 * i);
            }
            viderLeCache();

            assertThat(services.caissierDashboardService.getVentesRecentes(2)).hasSize(2);
            assertThat(services.caissierDashboardService.getVentesRecentes(null)).hasSize(5);
        }

        @Test
        @DisplayName("les ventes d'un autre caissier restent invisibles")
        void ventesDUnAutreCaissierInvisibles() {
            CashRegister caisse = caisseOuverte(0L);
            venteComptant(caisse, 10_000);
            viderLeCache();

            SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("admin", null, List.of()));

            assertThat(services.caissierDashboardService.getVentesRecentes(8)).isEmpty();
        }
    }

    // ===== assemblage =====

    @Nested
    @DisplayName("Assemblage")
    class Assemblage {

        @Test
        @DisplayName("un seul appel rend les cinq blocs cohérents entre eux")
        void assemblageComplet() {
            CashRegister caisse = caisseOuverte(50_000L);
            encaissement(caisse, "CASH", TypeFinancialTransaction.CASH_SALE, 100_000);
            venteComptant(caisse, 100_000);
            venteDifferee(caisse, 30_000, 30_000, client("Kouassi", "Yao", "0102030405"), aujourdHui());
            commande(fournisseur("LABOREX " + unique("")), OrderStatut.REQUESTED, 2);
            viderLeCache();

            CaissierDashboardDTO tableau = services.caissierDashboardService.getDashboardData();

            assertThat(tableau.caisseStatus().especesTheoriques()).isEqualTo(150_000L);
            assertThat(tableau.sessionEncaissements().totalEncaisse()).isEqualTo(100_000L);
            assertThat(tableau.sessionEncaissements().nombreTransactions()).isEqualTo(2);
            assertThat(tableau.resumeDifferes().montantTotalDu()).isEqualTo(30_000L);
            assertThat(tableau.livraisonsAttendues()).hasSize(1);
            assertThat(tableau.ventesRecentes()).hasSize(2);
        }
    }
}
