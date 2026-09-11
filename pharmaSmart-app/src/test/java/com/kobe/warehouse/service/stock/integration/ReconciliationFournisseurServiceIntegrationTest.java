package com.kobe.warehouse.service.stock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.ReconciliationStatut;
import com.kobe.warehouse.service.dto.ReconciliationFactureDTO;
import com.kobe.warehouse.service.stock.ReconciliationFournisseurService.ReconciliationCommand;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link com.kobe.warehouse.service.stock.ReconciliationFournisseurService} sur un vrai PostgreSQL.
 *
 * <p>Rapprocher la facture du fournisseur avec le bon de livraison, c'est comparer ce que le
 * fournisseur facture à ce que la commande a réellement enregistré. Les montants du BL viennent de
 * la base, pas de l'appelant, et la réconciliation est une entité en {@code OneToOne} avec cascade
 * portée par la commande : ce que la seconde saisie fait de la première — la remplacer ou l'écraser
 * — ne s'observe qu'en relisant.
 */
@DisplayName("ReconciliationFournisseurService — rapprochement facture/BL sur PostgreSQL")
class ReconciliationFournisseurServiceIntegrationTest extends AbstractStockIntegrationTest {

    @Test
    @DisplayName("Une facture conforme au bon de livraison est marquée réconciliée")
    void factureConforme() {
        CommandeId commande = bonDeLivraison(120_000, 21_600);
        viderLeCache();

        ReconciliationFactureDTO rapprochement = services.reconciliationFournisseurService.save(
            commande,
            new ReconciliationCommand("FA-2026-001", LocalDate.now(), 120_000, 21_600)
        );
        viderLeCache();

        assertEquals(ReconciliationStatut.RECONCILIEE, rapprochement.getStatut());
        assertEquals(0, rapprochement.getEcartHT());
        assertEquals(0, rapprochement.getEcartTVA());
        assertEquals(1, compter("SELECT count(*) FROM reconciliation_facture_fournisseur"));
    }

    @Test
    @DisplayName("Un écart de montant chiffre la différence et signale l'écart")
    void factureEnEcart() {
        CommandeId commande = bonDeLivraison(100_000, 18_000);
        viderLeCache();

        ReconciliationFactureDTO rapprochement = services.reconciliationFournisseurService.save(
            commande,
            new ReconciliationCommand("FA-2026-002", LocalDate.now(), 115_000, 20_700)
        );
        viderLeCache();

        assertEquals(ReconciliationStatut.ECART, rapprochement.getStatut());
        assertEquals(15_000, rapprochement.getEcartHT(), "le fournisseur facture 15 000 de plus que le BL");
        assertEquals(2_700, rapprochement.getEcartTVA());
        assertEquals(100_000, rapprochement.getBlMontantHT(), "le montant du BL vient de la commande, pas de la saisie");
    }

    @Test
    @DisplayName("Une facture inférieure au bon de livraison donne un écart négatif")
    void factureInferieureAuBon() {
        CommandeId commande = bonDeLivraison(80_000, 14_400);
        viderLeCache();

        ReconciliationFactureDTO rapprochement = services.reconciliationFournisseurService.save(
            commande,
            new ReconciliationCommand("FA-2026-003", LocalDate.now(), 75_000, 13_500)
        );

        assertEquals(-5_000, rapprochement.getEcartHT());
        assertEquals(-900, rapprochement.getEcartTVA());
        assertEquals(ReconciliationStatut.ECART, rapprochement.getStatut());
    }

    @Test
    @DisplayName("Une facture sans montant se compare à zéro")
    void factureSansMontant() {
        CommandeId commande = bonDeLivraison(50_000, 9_000);
        viderLeCache();

        ReconciliationFactureDTO rapprochement = services.reconciliationFournisseurService.save(
            commande,
            new ReconciliationCommand("FA-2026-004", LocalDate.now(), null, null)
        );

        assertEquals(0, rapprochement.getFactureMontantHT());
        assertEquals(-50_000, rapprochement.getEcartHT());
        assertEquals(ReconciliationStatut.ECART, rapprochement.getStatut());
    }

    @Test
    @DisplayName("Un second rapprochement corrige le premier sans en créer un autre")
    void secondRapprochement() {
        CommandeId commande = bonDeLivraison(90_000, 16_200);
        services.reconciliationFournisseurService.save(
            commande,
            new ReconciliationCommand("FA-2026-005", LocalDate.now(), 95_000, 16_200)
        );
        viderLeCache();

        ReconciliationFactureDTO corrige = services.reconciliationFournisseurService.save(
            commande,
            new ReconciliationCommand("FA-2026-005-BIS", LocalDate.now(), 90_000, 16_200)
        );
        viderLeCache();

        assertEquals(ReconciliationStatut.RECONCILIEE, corrige.getStatut());
        assertEquals("FA-2026-005-BIS", corrige.getFactureReference());
        assertNotNull(corrige.getUpdatedAt(), "la correction date la mise à jour");
        assertEquals(
            1,
            compter("SELECT count(*) FROM reconciliation_facture_fournisseur"),
            "la commande n'a qu'un rapprochement, il est réécrit"
        );
    }

    @Test
    @DisplayName("Le rapprochement d'une commande se relit par son identifiant composite")
    void relecture() {
        CommandeId commande = bonDeLivraison(60_000, 10_800);
        services.reconciliationFournisseurService.save(
            commande,
            new ReconciliationCommand("FA-2026-006", LocalDate.now(), 60_000, 10_800)
        );
        viderLeCache();

        ReconciliationFactureDTO relu = services.reconciliationFournisseurService.findByCommandeId(commande);

        assertEquals("FA-2026-006", relu.getFactureReference());
        assertEquals(ReconciliationStatut.RECONCILIEE, relu.getStatut());
    }

    @Test
    @DisplayName("Une commande jamais rapprochée ne rend rien")
    void relectureSansRapprochement() {
        CommandeId commande = bonDeLivraison(40_000, 7_200);
        viderLeCache();

        assertNull(services.reconciliationFournisseurService.findByCommandeId(commande));
    }

    @Test
    @DisplayName("Rapprocher une commande inexistante est refusé")
    void commandeInconnue() {
        CommandeId inconnue = new CommandeId(-1, LocalDate.now());
        ReconciliationCommand commande = new ReconciliationCommand("FA-X", LocalDate.now(), 1_000, 180);

        assertThrows(EntityNotFoundException.class, () -> services.reconciliationFournisseurService.save(inconnue, commande));
        assertThrows(EntityNotFoundException.class, () -> services.reconciliationFournisseurService.findByCommandeId(inconnue));
    }

    private CommandeId bonDeLivraison(int montantHt, int tva) {
        Produit produit = produitEnStock(unique("PRODUIT"), 100);
        FournisseurProduit fournisseurProduit = fournisseurProduit(produit);
        Commande commande = commande(fournisseurProduit.getFournisseur(), OrderStatut.RECEIVED);
        commande.setGrossAmount(montantHt);
        commande.setTaxAmount(tva);
        ligneDeCommande(commande, fournisseurProduit, 10, 10);
        return commande.getId();
    }
}
