package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.service.dto.report.ClientRetentionKpiDTO;
import com.kobe.warehouse.service.dto.report.ClientRetentionRowDTO;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * La rétention client répond à une question simple et coûteuse : qui ne revient plus ? Une officine
 * de quartier vit de ses habitués, et un client parti depuis trois mois est généralement parti chez
 * le concurrent. L'écran classe donc chacun selon son absence — actif sous trente jours, à risque
 * jusqu'à quatre-vingt-dix, perdu au-delà — pour que le pharmacien sache qui rappeler.
 *
 * <p>Les bornes sont tout : un client rangé « actif » alors qu'il est parti depuis deux mois ne sera
 * jamais rappelé. Elles se calculent en jours pleins, {@code CURRENT_DATE - derniere_visite}, et ce
 * sont elles que ces tests tiennent.
 *
 * <p>Le reste tient dans le <b>périmètre</b> : une vente anonyme n'appartient à personne et ne doit
 * pas créer de client fantôme ; une vente annulée ne prouve pas une visite.
 */
@DisplayName("ClientRetentionReportService — rétention et réveil des clients")
class ClientRetentionReportServiceIntegrationTest extends AbstractReportIntegrationTest {

    // ===== indicateurs =====

    @Nested
    @DisplayName("Indicateurs")
    class Indicateurs {

        /**
         * Les trois tranches sont éprouvées sur leurs bornes exactes — trente et quatre-vingt-dix
         * jours — parce que c'est là, et nulle part ailleurs, qu'un client change de statut.
         */
        @Test
        @DisplayName("classe chaque client selon la durée de son absence")
        void classementParAbsence() {
            visite(client("ACTIF", "Jean", "0101"), ilYA(30), 10_000);
            visite(client("RISQUE", "Marie", "0102"), ilYA(31), 10_000);
            visite(client("RISQUE LIMITE", "Paul", "0103"), ilYA(90), 10_000);
            visite(client("PERDU", "Anne", "0104"), ilYA(91), 10_000);
            viderLeCache();

            ClientRetentionKpiDTO kpi = services.clientRetentionReportService.getKpi();

            assertThat(kpi.totalClients()).isEqualTo(4);
            assertThat(kpi.clientsActifs()).isEqualTo(1);
            assertThat(kpi.clientsARisque()).isEqualTo(2);
            assertThat(kpi.clientsPerdus()).isEqualTo(1);
        }

        /** Seule la dernière visite compte : un client revenu hier est actif, quelle que soit son histoire. */
        @Test
        @DisplayName("c'est la dernière visite qui classe, pas la première")
        void seuleLaDerniereVisiteCompte() {
            UninsuredCustomer client = client("REVENU", "Jean", "0101");
            visite(client, ilYA(300), 10_000);
            visite(client, ilYA(1), 10_000);
            viderLeCache();

            ClientRetentionKpiDTO kpi = services.clientRetentionReportService.getKpi();

            assertThat(kpi.totalClients()).isEqualTo(1);
            assertThat(kpi.clientsActifs()).isEqualTo(1);
            assertThat(kpi.clientsPerdus()).isZero();
        }

        @Test
        @DisplayName("le chiffre d'affaires moyen rapporte le total au nombre de clients")
        void chiffreDAffairesMoyen() {
            UninsuredCustomer gros = client("GROS", "Jean", "0101");
            visite(gros, ilYA(5), 60_000);
            visite(gros, ilYA(10), 40_000);

            visite(client("PETIT", "Marie", "0102"), ilYA(5), 50_000);
            viderLeCache();

            // 100 000 pour l'un, 50 000 pour l'autre : 75 000 en moyenne.
            assertThat(services.clientRetentionReportService.getKpi().caMoyenParClient()).isEqualTo(75_000L);
        }

        @Test
        @DisplayName("les taux dérivés se calculent sans division par zéro")
        void tauxDerives() {
            visite(client("ACTIF", "Jean", "0101"), ilYA(5), 10_000);
            visite(client("PERDU", "Marie", "0102"), ilYA(200), 10_000);
            viderLeCache();

            ClientRetentionKpiDTO kpi = services.clientRetentionReportService.getKpi();

            assertThat(kpi.tauxActifs()).isEqualTo(50.0);
            assertThat(kpi.tauxPerdus()).isEqualTo(50.0);
            assertThat(kpi.tauxRisque()).isZero();
        }

        @Test
        @DisplayName("base vierge : des zéros, et des taux à zéro plutôt qu'une division impossible")
        void baseVierge() {
            ClientRetentionKpiDTO kpi = services.clientRetentionReportService.getKpi();

            assertThat(kpi.totalClients()).isZero();
            assertThat(kpi.clientsActifs()).isZero();
            assertThat(kpi.caMoyenParClient()).isZero();
            assertThat(kpi.tauxActifs()).isZero();
        }
    }

    // ===== liste de rappel =====

    @Nested
    @DisplayName("Liste de rappel")
    class ListeDeRappel {

        /** Les plus longtemps absents en tête : c'est l'ordre dans lequel on passe les appels. */
        @Test
        @DisplayName("les clients les plus longtemps absents passent en tête")
        void ordreParAbsenceDecroissante() {
            visite(client("RECENT", "Jean", "0101"), ilYA(5), 10_000);
            visite(client("ANCIEN", "Marie", "0102"), ilYA(200), 10_000);
            visite(client("MOYEN", "Paul", "0103"), ilYA(60), 10_000);
            viderLeCache();

            assertThat(services.clientRetentionReportService.getClientList(10))
                .extracting(ClientRetentionRowDTO::nom)
                .containsExactly("Marie ANCIEN", "Paul MOYEN", "Jean RECENT");
        }

        @Test
        @DisplayName("chaque ligne porte l'historique du client et son segment")
        void historiqueDuClient() {
            UninsuredCustomer client = client("KOUASSI", "Jean", "0102030405");
            visite(client, ilYA(200), 30_000);
            visite(client, ilYA(100), 20_000);
            viderLeCache();

            ClientRetentionRowDTO ligne = services.clientRetentionReportService.getClientList(10).getFirst();

            assertThat(ligne.nom()).isEqualTo("Jean KOUASSI");
            assertThat(ligne.premiereVisite()).isEqualTo(ilYA(200));
            assertThat(ligne.derniereVisite()).isEqualTo(ilYA(100));
            assertThat(ligne.nbAchats()).isEqualTo(2);
            assertThat(ligne.caTotal()).isEqualTo(50_000L);
            assertThat(ligne.joursAbsence()).isEqualTo(100);
            assertThat(ligne.segment()).isEqualTo("PERDU");
        }

        @Test
        @DisplayName("le segment de la ligne suit les mêmes bornes que les indicateurs")
        void segmentCoherentAvecLesIndicateurs() {
            visite(client("ACTIF", "Jean", "0101"), ilYA(30), 10_000);
            visite(client("RISQUE", "Marie", "0102"), ilYA(90), 10_000);
            visite(client("PERDU", "Paul", "0103"), ilYA(91), 10_000);
            viderLeCache();

            assertThat(services.clientRetentionReportService.getClientList(10))
                .extracting(ClientRetentionRowDTO::segment)
                .containsExactly("PERDU", "RISQUE", "ACTIF");
        }

        @Test
        @DisplayName("la limite borne la liste sans en changer l'ordre")
        void limite() {
            visite(client("RECENT", "Jean", "0101"), ilYA(5), 10_000);
            visite(client("ANCIEN", "Marie", "0102"), ilYA(200), 10_000);
            visite(client("MOYEN", "Paul", "0103"), ilYA(60), 10_000);
            viderLeCache();

            assertThat(services.clientRetentionReportService.getClientList(2))
                .extracting(ClientRetentionRowDTO::nom)
                .containsExactly("Marie ANCIEN", "Paul MOYEN");
        }

        @Test
        @DisplayName("base vierge : aucune ligne de rappel")
        void baseVierge() {
            assertThat(services.clientRetentionReportService.getClientList(10)).isEmpty();
        }
    }

    // ===== périmètre =====

    @Nested
    @DisplayName("Périmètre")
    class Perimetre {

        /** Une vente anonyme n'appartient à personne : elle ne doit pas créer de client fantôme. */
        @Test
        @DisplayName("une vente anonyme ne crée pas de client")
        void venteAnonymeIgnoree() {
            venteFermee(ilYA(5), false, CategorieChiffreAffaire.CA);
            viderLeCache();

            assertThat(services.clientRetentionReportService.getKpi().totalClients()).isZero();
            assertThat(services.clientRetentionReportService.getClientList(10)).isEmpty();
        }

        @Test
        @DisplayName("une vente annulée ne prouve pas une visite")
        void venteAnnuleeIgnoree() {
            UninsuredCustomer client = client("KOUASSI", "Jean", "0101");
            visite(client, ilYA(200), 10_000);
            venteFermee(ilYA(1), true, CategorieChiffreAffaire.CA, client);
            viderLeCache();

            ClientRetentionKpiDTO kpi = services.clientRetentionReportService.getKpi();

            assertThat(kpi.clientsPerdus()).isEqualTo(1);
            assertThat(kpi.clientsActifs()).isZero();
        }

        @Test
        @DisplayName("une vente hors chiffre d'affaires ne prouve pas non plus une visite")
        void venteHorsChiffreDAffairesIgnoree() {
            UninsuredCustomer client = client("KOUASSI", "Jean", "0101");
            visite(client, ilYA(200), 10_000);
            venteFermee(ilYA(1), false, CategorieChiffreAffaire.CA_DEPOT, client);
            viderLeCache();

            assertThat(services.clientRetentionReportService.getKpi().clientsPerdus()).isEqualTo(1);
        }
    }

    // ===== fabriques locales =====

    private static LocalDate ilYA(int jours) {
        return LocalDate.now().minusDays(jours);
    }

    /**
     * Une visite du client, portant une ligne de vente.
     *
     * <p>La ligne n'est pas décorative : les deux requêtes du service joignent {@code sales_line},
     * et un client dont les ventes n'auraient aucune ligne disparaîtrait purement et simplement du
     * rapport — y compris du comptage des clients.
     */
    private void visite(UninsuredCustomer client, LocalDate date, int montant) {
        var vente = venteFermee(date, false, CategorieChiffreAffaire.CA, client);
        var produit = produit(unique("PRODUIT"), TypeProduit.PACKAGE, montant, 5);
        ligneDeVente(vente, produit, 1);
        vente.setSalesAmount(montant);
        vente.setNetAmount(montant);
        vente.setAmountToBePaid(montant);
        vente.setPayrollAmount(montant);
        em.flush();
    }
}
