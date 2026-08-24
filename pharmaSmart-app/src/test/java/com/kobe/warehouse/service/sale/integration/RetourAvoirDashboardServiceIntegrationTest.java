package com.kobe.warehouse.service.sale.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.ModeReglementRetour;
import com.kobe.warehouse.domain.enumeration.MotifRetourClient;
import com.kobe.warehouse.service.sale.dto.RetourAvoirStatsDTO;
import com.kobe.warehouse.service.sale.dto.RetourClientRequest;
import com.kobe.warehouse.service.sale.dto.RetourClientRequest.RetourLineRequest;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link com.kobe.warehouse.service.sale.RetourAvoirDashboardService} sur un vrai PostgreSQL.
 *
 * <p>Ce tableau de bord n'est fait que d'agrégations : des {@code GROUP BY} avec {@code HAVING} qui
 * remontent des {@code Object[]} bruts, ensuite recastés à la main. Un double mémoire ne dirait rien
 * de l'ordre des colonnes ni du type réellement rendu par Postgres — {@code COUNT} sort en
 * {@code Long}, {@code SUM} en {@code BigDecimal} ou {@code Long} selon le cas — et c'est
 * exactement là que ces tableaux de bord se cassent.
 */
@DisplayName("RetourAvoirDashboardService — statistiques retours et avoirs sur PostgreSQL")
class RetourAvoirDashboardServiceIntegrationTest extends AbstractSaleIntegrationTest {

    @Test
    @DisplayName("Un mois sans mouvement rend des compteurs à zéro plutôt qu'une erreur")
    void moisVide() {
        RetourAvoirStatsDTO stats = services.retourAvoirDashboardService.getStats(YearMonth.now().minusYears(2));

        assertEquals(0, stats.nbRetoursMois());
        assertEquals(0, stats.montantTotalRetoursMois());
        assertTrue(stats.statsParMotif().isEmpty());
        assertTrue(stats.produitsEnAlerte().isEmpty());
    }

    @Test
    @DisplayName("Les retours du mois sont comptés et leur montant totalisé")
    void comptageDesRetours() {
        Produit produit = produitEnStock("STAT RETOUR", 2_000, 1_200, 0, 50);
        retour(produit, 2);
        retour(produit, 3);

        RetourAvoirStatsDTO stats = services.retourAvoirDashboardService.getStats(YearMonth.now());

        assertEquals(2, stats.nbRetoursMois());
        assertEquals(10_000, stats.montantTotalRetoursMois(), "2 × 2 000 + 3 × 2 000");
    }

    @Test
    @DisplayName("La répartition par motif rend le motif tel qu'il est stocké, pas sa chaîne")
    void repartitionParMotif() {
        Produit produit = produitEnStock("STAT MOTIF", 1_000, 600, 0, 50);
        retour(produit, 1, MotifRetourClient.ERREUR_DISPENSATION);
        retour(produit, 1, MotifRetourClient.ERREUR_DISPENSATION);
        retour(produit, 1, MotifRetourClient.PRODUIT_DEFECTUEUX);

        RetourAvoirStatsDTO stats = services.retourAvoirDashboardService.getStats(YearMonth.now());

        assertEquals(2, stats.statsParMotif().size());
        assertEquals(MotifRetourClient.ERREUR_DISPENSATION, stats.statsParMotif().getFirst().motif(), "le motif le plus fréquent d'abord");
        assertEquals(2, stats.statsParMotif().getFirst().count());
    }

    @Test
    @DisplayName("Un produit rendu au-delà du seuil passe en alerte, les autres non")
    void produitEnAlerte() {
        Produit suspect = produitEnStock("PRODUIT SUSPECT", 3_000, 1_800, 0, 100);
        Produit ordinaire = produitEnStock("PRODUIT ORDINAIRE", 3_000, 1_800, 0, 100);
        for (int i = 0; i < 6; i++) {
            retour(suspect, 1);
        }
        for (int i = 0; i < 5; i++) {
            retour(ordinaire, 1);
        }

        RetourAvoirStatsDTO stats = services.retourAvoirDashboardService.getStats(YearMonth.now());

        assertEquals(1, stats.produitsEnAlerte().size(), "l'alerte demande strictement plus de 5 retours : 5 ne suffisent pas");
        assertEquals("PRODUIT SUSPECT", stats.produitsEnAlerte().getFirst().libelle());
        assertEquals(6, stats.produitsEnAlerte().getFirst().nbRetours());
    }

    @Test
    @DisplayName("Un client qui rend trop souvent est signalé sous son nom complet")
    void clientEnAlerte() {
        Produit produit = produitEnStock("STAT CLIENT", 1_000, 600, 0, 100);
        UninsuredCustomer client = client("N'GUESSAN", "Paul", "CLI-STAT-1");
        for (int i = 0; i < 4; i++) {
            retour(produit, 1, MotifRetourClient.INSATISFACTION, client);
        }

        RetourAvoirStatsDTO stats = services.retourAvoirDashboardService.getStats(YearMonth.now());

        assertEquals(1, stats.clientsEnAlerte().size(), "strictement plus de 3 retours dans le mois");
        assertEquals("Paul N'GUESSAN", stats.clientsEnAlerte().getFirst().nom());
        assertEquals(4, stats.clientsEnAlerte().getFirst().nbRetours());
    }

    @Test
    @DisplayName("Les avoirs ouverts sont comptés avec leur reste à servir, hors avoirs soldés")
    void avoirsOuverts() {
        Produit produit = produitEnStock("STAT AVOIR", 5_000, 3_000, 0, 50);
        SalesLine premiere = venteFermee(produit, 5, 2).getSalesLines().iterator().next();
        SalesLine seconde = venteFermee(produit, 3, 1).getSalesLines().iterator().next();
        services.avoirClientDocumentService.createAvoirsFromSale(premiere, client("KABA", "Sita", "CLI-STAT-2"));
        services.avoirClientDocumentService.createAvoirsFromSale(seconde, client("BARRY", "Oumar", "CLI-STAT-3"));
        viderLeCache();

        RetourAvoirStatsDTO stats = services.retourAvoirDashboardService.getStats(YearMonth.now());

        assertEquals(2, stats.nbAvoirsOuverts());
        assertEquals(25_000, stats.montantTotalAvoirsOuverts(), "3 × 5 000 puis 2 × 5 000, rien encore utilisé");
        assertEquals(0, stats.nbAvoirsProchesExpiration(), "créés aujourd'hui pour 90 jours : aucun n'arrive à échéance");
    }

    @Test
    @DisplayName("Un avoir dont l'échéance approche est signalé")
    void avoirProcheDeLexpiration() {
        Produit produit = produitEnStock("STAT EXPIRE", 4_000, 2_400, 0, 50);
        SalesLine ligne = venteFermee(produit, 4, 1).getSalesLines().iterator().next();
        services.avoirClientDocumentService.createAvoirsFromSale(ligne, client("SANOGO", "Ali", "CLI-STAT-4"));
        viderLeCache();
        em.createNativeQuery(
            "UPDATE avoir_client SET date_expiration = DATE '" + LocalDate.now().plusDays(3) + "' WHERE sales_line_id = " +
            ligne.getId().getId()
        ).executeUpdate();
        viderLeCache();

        RetourAvoirStatsDTO stats = services.retourAvoirDashboardService.getStats(YearMonth.now());

        assertEquals(1, stats.nbAvoirsProchesExpiration(), "l'alerte porte sur les sept jours à venir");
    }

    // ===== outils =====

    private void retour(Produit produit, int quantite) {
        retour(produit, quantite, MotifRetourClient.ERREUR_DISPENSATION, null);
    }

    private void retour(Produit produit, int quantite, MotifRetourClient motif) {
        retour(produit, quantite, motif, null);
    }

    private void retour(Produit produit, int quantite, MotifRetourClient motif, UninsuredCustomer client) {
        CashSale vente = venteFermee(produit, quantite, quantite);
        if (client != null) {
            vente.setCustomer(client);
        }
        SalesLine ligne = vente.getSalesLines().iterator().next();
        viderLeCache();
        services.retourClientService.validerRetour(
            new RetourClientRequest(
                vente.getId().getId(),
                vente.getSaleDate(),
                motif,
                ModeReglementRetour.REMBOURSEMENT_ESPECES,
                null,
                List.of(new RetourLineRequest(ligne.getId().getId(), ligne.getSaleDate(), quantite, true, true, true)),
                false
            )
        );
        viderLeCache();
    }
}
