package com.kobe.warehouse.service.inventaire.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.domain.enumeration.InventoryCategory;
import com.kobe.warehouse.service.inventaire.InventoryClosedEvent;
import com.kobe.warehouse.service.inventaire.impl.InventoryClosedEventListener;
import com.kobe.warehouse.service.reassort.SuggestionReassortService;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Après une clôture, deux travaux sans rapport suivent : générer des suggestions de réassort, et
 * reporter les quantités comptées par lot dans {@code lot_stock_location}.
 *
 * <p>Le second était placé après le {@code return} du cas « aucune suggestion de réassort », et
 * derrière le filtre sur le type d'inventaire. Un inventaire dont les produits étaient tous
 * au-dessus de leur seuil — un stock sain — se clôturait donc sans jamais corriger les
 * emplacements de lots, qui gardaient leurs quantités d'avant le comptage. C'est précisément ce
 * que l'inventaire devait rectifier.
 *
 * <p>La fixture d'intégration double l'éditeur d'événements : le listener n'est exercé par aucun
 * autre test. On l'instancie donc directement, sur la vraie base.
 */
@DisplayName("Clôture d'inventaire — réconciliation des emplacements de lots")
class InventoryClosedEventListenerIntegrationTest extends AbstractInventaireIntegrationTest {

    private final SuggestionReassortService suggestionReassortService = mock(SuggestionReassortService.class);

    @Test
    @DisplayName("les emplacements de lots sont réconciliés même sans suggestion de réassort")
    void reconciliationSansSuggestion() {
        StoreInventory inventaire = inventaireAvecLotCompte(InventoryCategory.RAYON, 7);

        ecouter(inventaire, InventoryCategory.RAYON);

        assertEquals(7L, quantiteEnEmplacement(inventaire),
            "le comptage doit atteindre lot_stock_location");
        // Aucune ligne de réserve n'existe : les deux requêtes de suggestion ne remontent rien.
        verifyNoInteractions(suggestionReassortService);
    }

    /**
     * Un inventaire de périmés est exclu des suggestions de réassort — son objet est le retrait,
     * pas le réapprovisionnement. Mais c'est justement un inventaire de lots : ses comptages
     * doivent atteindre les emplacements.
     */
    @Test
    @DisplayName("un type exclu du réassort réconcilie quand même ses lots")
    void reconciliationSurTypeExcluDuReassort() {
        StoreInventory inventaire = inventaireAvecLotCompte(InventoryCategory.PERIME, 3);

        ecouter(inventaire, InventoryCategory.PERIME);

        assertEquals(3L, quantiteEnEmplacement(inventaire));
    }

    @Test
    @DisplayName("un lot compté à zéro sort des emplacements")
    void lotCompteAZeroEstRetire() {
        StoreInventory inventaire = inventaireAvecLotCompte(InventoryCategory.RAYON, 5);
        ecouter(inventaire, InventoryCategory.RAYON);
        assertEquals(5L, quantiteEnEmplacement(inventaire));

        em.createNativeQuery(
                "UPDATE inventory_lot SET quantity_on_hand = 0 WHERE store_inventory_line_id IN "
                    + "(SELECT id FROM store_inventory_line WHERE store_inventory_id = :id)")
            .setParameter("id", inventaire.getId())
            .executeUpdate();
        viderLeCache();

        ecouter(inventaire, InventoryCategory.RAYON);

        assertEquals(0L, lignesEnEmplacement(inventaire), "le lot vidé ne doit plus figurer");
    }

    // ===== utilitaires =====

    private void ecouter(StoreInventory inventaire, InventoryCategory categorie) {
        new InventoryClosedEventListener(em, suggestionReassortService).onInventoryClosed(
            new InventoryClosedEvent(
                inventaire.getId(),
                categorie,
                rayon.getStorageType(),
                rayon.getId(),
                magasin.getId(),
                utilisateur.getId()
            )
        );
        viderLeCache();
    }

    /** Un inventaire d'une seule ligne, dont l'unique lot a été compté. */
    private StoreInventory inventaireAvecLotCompte(InventoryCategory categorie, int quantiteComptee) {
        Produit produit = produitEnStock(unique("DOLIPRANE LOT"), 50);
        StoreInventory inventaire = inventaire(categorie);
        StoreInventoryLine ligne = ligneComptee(inventaire, produit, 50, quantiteComptee);
        Lot lot = lot(produit, unique("L"), LocalDate.now().plusMonths(6), 50);
        ligneDeLot(ligne, lot, quantiteComptee);
        viderLeCache();
        return inventaire;
    }

    private long quantiteEnEmplacement(StoreInventory inventaire) {
        return compter(
            """
            SELECT COALESCE(sum(lsl.qty), 0) FROM lot_stock_location lsl
            JOIN inventory_lot il ON il.lot_id = lsl.lot_id
            JOIN store_inventory_line sil ON sil.id = il.store_inventory_line_id
            WHERE sil.store_inventory_id = %d AND lsl.storage_id = %d
            """.formatted(inventaire.getId(), rayon.getId())
        );
    }

    private long lignesEnEmplacement(StoreInventory inventaire) {
        return compter(
            """
            SELECT count(*) FROM lot_stock_location lsl
            JOIN inventory_lot il ON il.lot_id = lsl.lot_id
            JOIN store_inventory_line sil ON sil.id = il.store_inventory_line_id
            WHERE sil.store_inventory_id = %d AND lsl.storage_id = %d
            """.formatted(inventaire.getId(), rayon.getId())
        );
    }
}
