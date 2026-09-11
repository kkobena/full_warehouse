package com.kobe.warehouse.service.inventaire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.enumeration.InventoryCategory;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.inventaire.impl.InventoryStockServiceImpl;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Le stock théorique est le chiffre auquel l'opérateur compare ce qu'il compte : l'écart en
 * découle entièrement. Deux choses s'y jouent — la portée (le storage inventorié seul, ou le
 * magasin entier) et l'absence de {@code NullPointerException} sur un produit sans stock, qui
 * est la situation ordinaire d'un inventaire de rupture.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryStockService — stock théorique servi à la saisie")
class InventoryStockServiceImplTest {

    private static final int STORAGE_RAYON = 1;
    private static final int MAGASIN = 7;

    @Mock
    private StockProduitRepository stockProduitRepository;

    private InventoryStockService service;

    @BeforeEach
    void init() {
        service = new InventoryStockServiceImpl(stockProduitRepository);
    }

    @Test
    @DisplayName("Un inventaire de rayon ne lit que le stock du rayon, pas celui de la réserve")
    void porteeRayon() {
        when(stockProduitRepository.findAllByStorageIdAndProduitIdIn(STORAGE_RAYON, Set.of(10)))
            .thenReturn(List.of(stock(10, 4, 1)));

        Map<Integer, Integer> stock = service.buildStockMapForInventory(
            inventaire(InventoryCategory.RAYON), Set.of(10));

        assertEquals(Map.of(10, 5), stock, "4 en stock + 1 en unité gratuite");
        verify(stockProduitRepository, never())
            .findAggregatedStockByMagasinIdAndProduitIdIn(anyInt(), any());
    }

    @Test
    @DisplayName("Un inventaire de magasin agrège rayon et réserve")
    void porteeMagasin() {
        when(stockProduitRepository.findAggregatedStockByMagasinIdAndProduitIdIn(MAGASIN, Set.of(10)))
            .thenReturn(List.<Object[]>of(new Object[] { 10, 42L }));

        Map<Integer, Integer> stock = service.buildStockMapForInventory(
            inventaire(InventoryCategory.MAGASIN), Set.of(10));

        assertEquals(Map.of(10, 42), stock);
        verify(stockProduitRepository, never())
            .findAllByStorageIdAndProduitIdIn(anyInt(), any());
    }

    @Test
    @DisplayName("Les types thématiques suivent la portée magasin, comme MAGASIN")
    void porteeThematique() {
        when(stockProduitRepository.findAggregatedStockByMagasinIdAndProduitIdIn(eq(MAGASIN), any()))
            .thenReturn(List.<Object[]>of(new Object[] { 10, 3L }));

        for (InventoryCategory categorie : List.of(
            InventoryCategory.FAMILLY,
            InventoryCategory.PERIME,
            InventoryCategory.ALERTE_PEREMPTION,
            InventoryCategory.VENDU,
            InventoryCategory.INVENDU,
            InventoryCategory.SOUS_SEUIL,
            InventoryCategory.EN_RUPTURE,
            InventoryCategory.ABC,
            InventoryCategory.SELECTION_PRODUIT
        )) {
            assertEquals(
                Map.of(10, 3),
                service.buildStockMapForInventory(inventaire(categorie), Set.of(10)),
                categorie + " compte le stock du magasin entier"
            );
        }
    }

    @Test
    @DisplayName("Un inventaire sans emplacement ne provoque aucune requête")
    void inventaireSansStorage() {
        StoreInventory sansStorage = new StoreInventory();
        sansStorage.setInventoryCategory(InventoryCategory.MAGASIN);

        assertTrue(service.buildStockMapForInventory(sansStorage, Set.of(10)).isEmpty());
        assertTrue(service.buildStockMapForInventory(null, Set.of(10)).isEmpty());
        verifyNoInteractions(stockProduitRepository);
    }

    @Test
    @DisplayName("Une page vide ne provoque aucune requête")
    void aucunProduit() {
        assertTrue(service.buildStockMapByStorage(STORAGE_RAYON, Set.of()).isEmpty());
        assertTrue(service.buildStockMapByStorage(STORAGE_RAYON, null).isEmpty());
        assertTrue(service.buildStockMapByMagasin(MAGASIN, Set.of()).isEmpty());
        assertTrue(service.buildStockMapByMagasin(MAGASIN, null).isEmpty());
        verifyNoInteractions(stockProduitRepository);
    }

    @Test
    @DisplayName("Une unité gratuite absente vaut zéro, elle n'interrompt pas le chargement")
    void quantiteUgNulle() {
        when(stockProduitRepository.findAllByStorageIdAndProduitIdIn(STORAGE_RAYON, Set.of(10)))
            .thenReturn(List.of(stock(10, 6, null)));

        assertEquals(Map.of(10, 6), service.buildStockMapByStorage(STORAGE_RAYON, Set.of(10)));
    }

    @Test
    @DisplayName("Deux lignes de stock pour le même produit s'additionnent au lieu de s'écraser")
    void deuxLignesPourLeMemeProduit() {
        when(stockProduitRepository.findAllByStorageIdAndProduitIdIn(STORAGE_RAYON, Set.of(10)))
            .thenReturn(List.of(stock(10, 4, 0), stock(10, 6, 0)));

        assertEquals(Map.of(10, 10), service.buildStockMapByStorage(STORAGE_RAYON, Set.of(10)));
    }

    @Test
    @DisplayName("Une somme agrégée nulle est ramenée à zéro")
    void agregatNul() {
        when(stockProduitRepository.findAggregatedStockByMagasinIdAndProduitIdIn(MAGASIN, Set.of(10)))
            .thenReturn(List.<Object[]>of(new Object[] { 10, null }));

        assertEquals(Map.of(10, 0), service.buildStockMapByMagasin(MAGASIN, Set.of(10)));
    }

    @Test
    @DisplayName("Un produit sans ligne de stock vaut zéro plutôt que de lever une NPE")
    void produitSansStock() {
        when(stockProduitRepository.findOneByProduitIdAndStockageId(10, STORAGE_RAYON)).thenReturn(null);
        when(stockProduitRepository.findTotalQuantityByMagasinIdIdAndProduitId(MAGASIN, 10)).thenReturn(null);

        assertEquals(0, service.getStockByStorage(STORAGE_RAYON, 10));
        assertEquals(0, service.getStockByMagasin(MAGASIN, 10));
    }

    @Test
    @DisplayName("Le stock unitaire additionne les unités gratuites")
    void stockUnitaire() {
        when(stockProduitRepository.findOneByProduitIdAndStockageId(10, STORAGE_RAYON))
            .thenReturn(stock(10, 8, 2));
        when(stockProduitRepository.findTotalQuantityByMagasinIdIdAndProduitId(MAGASIN, 10))
            .thenReturn(30);

        assertEquals(10, service.getStockByStorage(STORAGE_RAYON, 10));
        assertEquals(30, service.getStockByMagasin(MAGASIN, 10));
    }

    // ===== jeu d'essai =====

    private StoreInventory inventaire(InventoryCategory categorie) {
        Magasin magasin = new Magasin();
        magasin.setId(MAGASIN);
        Storage storage = new Storage();
        storage.setId(STORAGE_RAYON);
        storage.setMagasin(magasin);

        StoreInventory inventaire = new StoreInventory();
        inventaire.setStorage(storage);
        inventaire.setInventoryCategory(categorie);
        return inventaire;
    }

    private StockProduit stock(int produitId, Integer quantite, Integer unitesGratuites) {
        Produit produit = new Produit();
        produit.setId(produitId);
        StockProduit stockProduit = new StockProduit();
        stockProduit.setProduit(produit);
        stockProduit.setQtyStock(quantite);
        stockProduit.setQtyUG(unitesGratuites);
        return stockProduit;
    }
}
