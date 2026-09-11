package com.kobe.warehouse.service.inventaire.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.enumeration.InventoryCategory;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le stock théorique se lit dans deux tables différentes selon la portée : une ligne de
 * {@code stock_produit} pour un inventaire de rayon, la somme des lignes du magasin sinon. Le
 * point sensible est là — un produit présent à la fois en rayon et en réserve doit donner deux
 * chiffres distincts selon l'inventaire, et c'est l'agrégat SQL qui en décide.
 */
@DisplayName("InventoryStockService — stock théorique lu sur PostgreSQL")
class InventoryStockServiceIntegrationTest extends AbstractInventaireIntegrationTest {

    @Test
    @DisplayName("Un inventaire de rayon ignore le stock de la réserve")
    void porteeRayon() {
        Produit produit = produit(unique("DOUBLE EMPLACEMENT"));
        stock(produit, rayon, 12);
        stock(produit, reserve, 40);
        viderLeCache();

        StoreInventory inventaire = inventaire(InventoryCategory.RAYON, rayon);

        assertEquals(
            Map.of(produit.getId(), 12),
            services.inventoryStockService.buildStockMapForInventory(inventaire, Set.of(produit.getId()))
        );
    }

    @Test
    @DisplayName("Un inventaire de magasin additionne rayon et réserve")
    void porteeMagasin() {
        Produit produit = produit(unique("DOUBLE EMPLACEMENT"));
        stock(produit, rayon, 12);
        stock(produit, reserve, 40);
        viderLeCache();

        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN, rayon);

        assertEquals(
            Map.of(produit.getId(), 52),
            services.inventoryStockService.buildStockMapForInventory(inventaire, Set.of(produit.getId()))
        );
    }

    @Test
    @DisplayName("Un produit sans ligne de stock est absent de la carte, sans faire échouer les autres")
    void produitSansStock() {
        Produit avecStock = produitEnStock(unique("EN STOCK"), 7);
        Produit sansStock = produit(unique("JAMAIS APPROVISIONNE"));
        viderLeCache();

        Map<Integer, Integer> carte = services.inventoryStockService.buildStockMapByStorage(
            STORAGE_RAYON_ID, Set.of(avecStock.getId(), sansStock.getId()));

        assertEquals(1, carte.size());
        assertEquals(7, carte.get(avecStock.getId()));
        assertEquals(0, services.inventoryStockService.getStockByStorage(STORAGE_RAYON_ID, sansStock.getId()));
        assertEquals(0, services.inventoryStockService.getStockByMagasin(MAGASIN_ID, sansStock.getId()));
    }

    @Test
    @DisplayName("Les unités gratuites comptent dans le stock théorique")
    void unitesGratuites() {
        Produit produit = produit(unique("AVEC UG"));
        var stockProduit = stock(produit, rayon, 10);
        stockProduit.setQtyUG(3);
        viderLeCache();

        assertEquals(13, services.inventoryStockService.getStockByStorage(STORAGE_RAYON_ID, produit.getId()));
        assertEquals(13, services.inventoryStockService.getStockByMagasin(MAGASIN_ID, produit.getId()));
    }

    @Test
    @DisplayName("Toute la page est chargée en une seule lecture, quel que soit le nombre de produits")
    void chargementEnMasse() {
        Produit premier = produitEnStock(unique("A"), 1);
        Produit second = produitEnStock(unique("B"), 2);
        Produit troisieme = produitEnStock(unique("C"), 3);
        viderLeCache();

        assertEquals(
            Map.of(premier.getId(), 1, second.getId(), 2, troisieme.getId(), 3),
            services.inventoryStockService.buildStockMapByStorage(
                STORAGE_RAYON_ID, Set.of(premier.getId(), second.getId(), troisieme.getId()))
        );
    }
}
