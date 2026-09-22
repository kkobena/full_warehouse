package com.kobe.warehouse.service.inventaire.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.domain.enumeration.InventoryCategory;
import com.kobe.warehouse.domain.enumeration.InventoryStatut;
import com.kobe.warehouse.service.dto.StoreInventoryLineDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Un inventaire clôturé a été appliqué au stock et a laissé une ligne dans
 * {@code historique_inventaire}. Ce n'est plus un brouillon : le supprimer effacerait la
 * justification d'un ajustement de stock en laissant son historique orphelin, et recompter une de
 * ses lignes la ferait diverger de ce qui a réellement été appliqué, sans que le stock bouge.
 *
 * <p>Ni la suppression ni le comptage ne vérifiaient le statut.
 */
@DisplayName("Inventaire clôturé — gestes refusés")
class InventaireServiceStatutIntegrationTest extends AbstractInventaireIntegrationTest {

    @Test
    @DisplayName("un inventaire clôturé ne peut pas être supprimé")
    void suppressionRefuseeSurInventaireCloture() {
        StoreInventory inventaire = inventaireCloture();
        Long id = inventaire.getId();
        viderLeCache();

        assertThrows(BadRequestAlertException.class, () -> services.inventaireService.remove(id));

        assertEquals(1L, compter(
            "SELECT count(*) FROM store_inventory WHERE id = " + id),
            "l'inventaire doit rester en base");
    }

    @Test
    @DisplayName("une ligne d'inventaire clôturé ne peut plus être comptée")
    void comptageRefuseSurInventaireCloture() {
        StoreInventory inventaire = inventaireCloture();
        StoreInventoryLine ligne = premiereLigne(inventaire);
        StoreInventoryLineDTO dto = new StoreInventoryLineDTO(ligne);
        dto.setQuantityOnHand(99);
        viderLeCache();

        assertThrows(BadRequestAlertException.class,
            () -> services.inventaireService.updateQuantityOnHand(dto));
    }

    /** La garde ne doit pas gêner le cas courant : un inventaire en cours reste modifiable. */
    @Test
    @DisplayName("un inventaire en cours reste supprimable")
    void suppressionAutoriseeSurInventaireEnCours() {
        StoreInventory inventaire = inventaireAvecUneLigne(InventoryStatut.PROCESSING);
        Long id = inventaire.getId();
        viderLeCache();

        services.inventaireService.remove(id);
        viderLeCache();

        assertEquals(0L, compter("SELECT count(*) FROM store_inventory WHERE id = " + id));
        assertEquals(0L, lignesDe(id), "ses lignes partent avec lui");
    }

    // ===== utilitaires =====

    private StoreInventory inventaireCloture() {
        return inventaireAvecUneLigne(InventoryStatut.CLOSED);
    }

    private StoreInventory inventaireAvecUneLigne(InventoryStatut statut) {
        Produit produit = produitEnStock(unique("DOLIPRANE STATUT"), 20);
        StoreInventory inventaire = inventaire(InventoryCategory.RAYON);
        ligneComptee(inventaire, produit, 20, 20);
        inventaire.setStatut(statut);
        em.flush();
        return inventaire;
    }

    private StoreInventoryLine premiereLigne(StoreInventory inventaire) {
        return em
            .createQuery(
                "SELECT l FROM StoreInventoryLine l WHERE l.storeInventory.id = :id",
                StoreInventoryLine.class)
            .setParameter("id", inventaire.getId())
            .getResultList()
            .getFirst();
    }
}
