package com.kobe.warehouse.service.inventaire.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.domain.enumeration.InventoryCategory;
import com.kobe.warehouse.domain.enumeration.InventoryStatut;
import com.kobe.warehouse.service.dto.records.ItemsCountRecord;
import com.kobe.warehouse.service.errors.InventoryException;
import com.kobe.warehouse.service.inventaire.InventoryClosedEvent;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La clôture appelle une procédure stockée : le stock écrit, les mouvements journalisés et la
 * remise à zéro de la réserve n'existent que dans du PL/pgSQL. C'est le seul endroit de
 * l'inventaire où une erreur est irréversible pour le pharmacien — d'où le soin porté ici aux
 * effets exacts sur {@code stock_produit} et {@code inventory_transaction}.
 */
@DisplayName("InventoryCloseService — clôture d'un inventaire sur PostgreSQL")
class InventoryCloseServiceIntegrationTest extends AbstractInventaireIntegrationTest {

    @Test
    @DisplayName("La clôture écrit le stock compté et journalise le mouvement")
    void clotureEcritLeStock() {
        StoreInventory inventaire = inventaire(InventoryCategory.STORAGE, rayon);
        Produit produit = produit(unique("MANQUANT"), 1_000, 600);
        stock(produit, rayon, 10);
        ligneComptee(inventaire, produit, 10, 7);
        viderLeCache();

        ItemsCountRecord resultat = services.inventoryCloseService.close(inventaire.getId());
        viderLeCache();

        assertEquals(1, resultat.count());
        assertEquals(
            7,
            compter("SELECT qty_stock FROM stock_produit WHERE produit_id = %d AND storage_id = %d"
                .formatted(produit.getId(), STORAGE_RAYON_ID)),
            "le stock du rayon vaut désormais le comptage"
        );
        assertEquals(
            1,
            compter("SELECT count(*) FROM inventory_transaction WHERE entity_id IN (SELECT id FROM store_inventory_line WHERE store_inventory_id = %d) AND mouvement_type = 'INVENTAIRE'"
                .formatted(inventaire.getId())),
            "le mouvement d'inventaire est journalisé"
        );
        assertEquals(
            -3,
            compter("SELECT quantity FROM inventory_transaction WHERE entity_id IN (SELECT id FROM store_inventory_line WHERE store_inventory_id = %d)"
                .formatted(inventaire.getId())),
            "le journal porte le delta signé, pas la quantité comptée"
        );
    }

    @Test
    @DisplayName("La clôture fige la valorisation dans l'entête et passe l'inventaire en clos")
    void clotureFigeLaValorisation() {
        StoreInventory inventaire = inventaire(InventoryCategory.STORAGE, rayon);
        Produit produit = produit(unique("MANQUANT"), 1_000, 600);
        stock(produit, rayon, 10);
        ligneComptee(inventaire, produit, 10, 7);
        viderLeCache();

        services.inventoryCloseService.close(inventaire.getId());
        viderLeCache();

        StoreInventory relu = services.storeInventoryRepository.findById(inventaire.getId()).orElseThrow();
        assertEquals(InventoryStatut.CLOSED, relu.getStatut());
        assertEquals(6_000L, relu.getInventoryValueCostBegin(), "10 × 600");
        assertEquals(4_200L, relu.getInventoryValueCostAfter(), "7 × 600");
        assertEquals(10_000L, relu.getInventoryAmountBegin());
        assertEquals(7_000L, relu.getInventoryAmountAfter());
        assertEquals(-1_800, relu.getGapCost());
        assertEquals(-3_000, relu.getGapAmount());
    }

    @Test
    @DisplayName("Une ligne non comptée bloque la clôture sans rien écrire")
    void ligneNonComptee() {
        StoreInventory inventaire = inventaire(InventoryCategory.STORAGE, rayon);
        Produit produit = produitEnStock(unique("OUBLIE"), 10);
        ligne(inventaire, produit);
        viderLeCache();

        assertThrows(InventoryException.class, () -> services.inventoryCloseService.close(inventaire.getId()));
        viderLeCache();

        assertEquals(
            10,
            compter("SELECT qty_stock FROM stock_produit WHERE produit_id = %d AND storage_id = %d"
                .formatted(produit.getId(), STORAGE_RAYON_ID)),
            "le stock n'a pas bougé"
        );
        assertEquals(InventoryStatut.CREATE,
            services.storeInventoryRepository.findById(inventaire.getId()).orElseThrow().getStatut());
    }

    @Test
    @DisplayName("Reclôturer un inventaire clos ne rejoue pas la procédure")
    void doubleCloture() {
        StoreInventory inventaire = inventaire(InventoryCategory.STORAGE, rayon);
        Produit produit = produit(unique("MANQUANT"), 1_000, 600);
        stock(produit, rayon, 10);
        ligneComptee(inventaire, produit, 10, 7);
        viderLeCache();

        services.inventoryCloseService.close(inventaire.getId());
        viderLeCache();
        ItemsCountRecord seconde = services.inventoryCloseService.close(inventaire.getId());
        viderLeCache();

        assertEquals(0, seconde.count());
        assertEquals(
            1,
            compter("SELECT count(*) FROM inventory_transaction WHERE entity_id IN (SELECT id FROM store_inventory_line WHERE store_inventory_id = %d)"
                .formatted(inventaire.getId())),
            "le mouvement n'est pas journalisé deux fois"
        );
        verify(services.eventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(InventoryClosedEvent.class));
    }

    @Test
    @DisplayName("Un inventaire magasin remet la réserve à zéro, son contenu ayant été compté avec le rayon")
    void clotureMagasinVideLaReserve() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN, rayon);
        Produit produit = produit(unique("CONSOLIDE"), 1_000, 600);
        stock(produit, rayon, 10);
        stock(produit, reserve, 40);
        ligneComptee(inventaire, produit, 50, 48);
        viderLeCache();

        services.inventoryCloseService.close(inventaire.getId());
        viderLeCache();

        assertEquals(48, compter("SELECT qty_stock FROM stock_produit WHERE produit_id = %d AND storage_id = %d"
            .formatted(produit.getId(), STORAGE_RAYON_ID)));
        assertEquals(0, compter("SELECT qty_stock FROM stock_produit WHERE produit_id = %d AND storage_id = %d"
            .formatted(produit.getId(), STORAGE_RESERVE_ID)), "la réserve est incluse dans le chiffre consolidé");
    }

    @Test
    @DisplayName("Un inventaire d'emplacement laisse la réserve intacte")
    void clotureEmplacementPreserveLaReserve() {
        StoreInventory inventaire = inventaire(InventoryCategory.STORAGE, rayon);
        Produit produit = produit(unique("RAYON SEUL"), 1_000, 600);
        stock(produit, rayon, 10);
        stock(produit, reserve, 40);
        ligneComptee(inventaire, produit, 10, 9);
        viderLeCache();

        services.inventoryCloseService.close(inventaire.getId());
        viderLeCache();

        assertEquals(40, compter("SELECT qty_stock FROM stock_produit WHERE produit_id = %d AND storage_id = %d"
            .formatted(produit.getId(), STORAGE_RESERVE_ID)), "la réserve n'était pas dans le périmètre");
    }

    @Test
    @DisplayName("En gestion de lot, la clôture reporte le comptage sur la quantité courante des lots")
    void clotureAvecGestionDeLot() {
        when(services.appConfigurationService.useGestionLotInventaire()).thenReturn(true);
        StoreInventory inventaire = inventaire(InventoryCategory.STORAGE, rayon);
        Produit produit = produit(unique("A LOTS"), 1_000, 600);
        stock(produit, rayon, 30);
        Lot premier = lot(produit, unique("LOT-A"), LocalDate.now().plusYears(1), 20);
        Lot second = lot(produit, unique("LOT-B"), LocalDate.now().plusYears(2), 10);
        StoreInventoryLine ligne = ligneComptee(inventaire, produit, 30, 26);
        ligneDeLot(ligne, premier, 18);
        ligneDeLot(ligne, second, 8);
        viderLeCache();

        services.inventoryCloseService.close(inventaire.getId());
        viderLeCache();

        assertEquals(18, compter("SELECT current_quantity FROM lot WHERE id = " + premier.getId()));
        assertEquals(8, compter("SELECT current_quantity FROM lot WHERE id = " + second.getId()));
    }

    @Test
    @DisplayName("Sans gestion de lot, la clôture ne touche pas aux quantités des lots")
    void clotureSansGestionDeLot() {
        StoreInventory inventaire = inventaire(InventoryCategory.STORAGE, rayon);
        Produit produit = produit(unique("A LOTS"), 1_000, 600);
        stock(produit, rayon, 30);
        Lot premier = lot(produit, unique("LOT-A"), LocalDate.now().plusYears(1), 20);
        StoreInventoryLine ligne = ligneComptee(inventaire, produit, 30, 26);
        ligneDeLot(ligne, premier, 18);
        viderLeCache();

        services.inventoryCloseService.close(inventaire.getId());
        viderLeCache();

        assertEquals(20, compter("SELECT current_quantity FROM lot WHERE id = " + premier.getId()));
    }

    @Test
    @DisplayName("L'événement de clôture porte le périmètre réellement inventorié")
    void evenementDeCloture() {
        StoreInventory inventaire = inventaire(InventoryCategory.STORAGE, rayon);
        Produit produit = produit(unique("MANQUANT"), 1_000, 600);
        stock(produit, rayon, 10);
        ligneComptee(inventaire, produit, 10, 7);
        viderLeCache();

        services.inventoryCloseService.close(inventaire.getId());

        var evenement = org.mockito.ArgumentCaptor.forClass(InventoryClosedEvent.class);
        verify(services.eventPublisher).publishEvent(evenement.capture());
        assertEquals(inventaire.getId(), evenement.getValue().storeInventoryId());
        assertEquals(InventoryCategory.STORAGE, evenement.getValue().inventoryCategory());
        assertEquals(STORAGE_RAYON_ID, evenement.getValue().storageId().intValue());
        assertEquals(MAGASIN_ID, evenement.getValue().magasinId().intValue());
        assertEquals(utilisateur.getId(), evenement.getValue().userId());
    }

    @Test
    @DisplayName("Une clôture refusée ne publie aucun événement de réassort")
    void aucunEvenementSiRefus() {
        StoreInventory inventaire = inventaire(InventoryCategory.STORAGE, rayon);
        ligne(inventaire, produitEnStock(unique("OUBLIE"), 10));
        viderLeCache();

        assertThrows(InventoryException.class, () -> services.inventoryCloseService.close(inventaire.getId()));

        verify(services.eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any(InventoryClosedEvent.class));
    }
}
