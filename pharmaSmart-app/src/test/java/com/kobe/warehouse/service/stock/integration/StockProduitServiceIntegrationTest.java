package com.kobe.warehouse.service.stock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.service.dto.StockProduitDTO;
import com.kobe.warehouse.service.stock.dto.StockProduitSearchDTO;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

/**
 * {@link com.kobe.warehouse.service.stock.StockProduitService} sur un vrai PostgreSQL.
 *
 * <p>Ce service crée et met à jour la ligne de stock d'un produit sur un emplacement. Ce qui
 * demande une base : la recherche pour répartition, qui joint produit, emplacement et référencement
 * fournisseur, filtre sur le magasin et n'accepte que le stock strictement positif — un ensemble de
 * conditions qui ne se vérifie qu'en les exécutant.
 */
@DisplayName("StockProduitService — stock par emplacement sur PostgreSQL")
class StockProduitServiceIntegrationTest extends AbstractStockIntegrationTest {

    @Test
    @DisplayName("Créer un stock de réserve l'écrit sur l'emplacement réserve")
    void creationDUnStockDeReserve() {
        Produit produit = produitEnStock("ADVIL", 40);
        viderLeCache();

        StockProduitDTO demande = new StockProduitDTO().setProduitId(produit.getId()).setQtyStock(25);
        demande.setSeuilMini(5);
        demande.setStockMaxi(100);
        demande.setStockReassort(30);

        StockProduitDTO cree = services.stockProduitService.createStockProduit(demande);
        viderLeCache();

        StockProduit relu = em.find(StockProduit.class, cree.getId());
        assertEquals(STORAGE_RESERVE_ID, relu.getStorage().getId());
        assertEquals(25, relu.getQtyStock());
        assertEquals(25, relu.getQtyVirtual(), "le stock virtuel part de la quantité physique");
        assertEquals(5, relu.getSeuilMini());
        assertEquals(100, relu.getStockMaxi());
        verify(services.repartitionStockService, never()).transferStockBetweenStorages(ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Un stock créé avec transfert déclenche la répartition entre emplacements")
    void creationAvecTransfert() {
        Produit produit = produitEnStock("NUROFEN", 40);
        viderLeCache();

        StockProduitDTO demande = new StockProduitDTO().setProduitId(produit.getId()).setQtyStock(10);
        demande.setWithTransfer(true);

        services.stockProduitService.createStockProduit(demande);
        viderLeCache();

        verify(services.repartitionStockService).transferStockBetweenStorages(ArgumentMatchers.any(StockProduit.class));
    }

    @Test
    @DisplayName("Créer un stock sur un produit inexistant est refusé")
    void creationSurProduitInconnu() {
        StockProduitDTO dto = new StockProduitDTO().setProduitId(-1).setQtyStock(10);

        assertThrows(EntityNotFoundException.class, () -> services.stockProduitService.createStockProduit(dto));
    }

    @Test
    @DisplayName("La mise à jour ne touche que les seuils, jamais les quantités")
    void miseAJourDesSeuils() {
        Produit produit = produit("ASPEGIC");
        StockProduit stock = stock(produit, rayon, 60);
        viderLeCache();

        StockProduitDTO demande = new StockProduitDTO().setId(stock.getId()).setQtyStock(9999);
        demande.setSeuilMini(12);
        demande.setStockMaxi(90);
        demande.setStockReassort(45);

        services.stockProduitService.updateStockProduit(demande);
        viderLeCache();

        StockProduit relu = em.find(StockProduit.class, stock.getId());
        assertEquals(12, relu.getSeuilMini());
        assertEquals(90, relu.getStockMaxi());
        assertEquals(45, relu.getStockReassort());
        assertEquals(60, relu.getQtyStock(), "la quantité du DTO est ignorée : elle ne se corrige pas ici");
    }

    @Test
    @DisplayName("Une mise à jour partielle laisse les seuils non renseignés en place")
    void miseAJourPartielle() {
        Produit produit = produit("DOLIRHUME");
        StockProduit stock = stock(produit, rayon, 30);
        stock.setSeuilMini(7);
        stock.setStockMaxi(70);
        viderLeCache();

        StockProduitDTO demande = new StockProduitDTO().setId(stock.getId());
        demande.setStockMaxi(80);

        services.stockProduitService.updateStockProduit(demande);
        viderLeCache();

        StockProduit relu = em.find(StockProduit.class, stock.getId());
        assertEquals(7, relu.getSeuilMini(), "un seuil absent du DTO n'est pas écrasé");
        assertEquals(80, relu.getStockMaxi());
    }

    @Test
    @DisplayName("Un stock inexistant ne se met pas à jour")
    void miseAJourSurStockInconnu() {
        StockProduitDTO dto = new StockProduitDTO().setId(-1);
        dto.setSeuilMini(3);

        assertThrows(EntityNotFoundException.class, () -> services.stockProduitService.updateStockProduit(dto));
    }

    @Test
    @DisplayName("Un stock se relit par son identifiant avec son emplacement et son produit")
    void lectureDUnStock() {
        Produit produit = produit("HUMEX");
        StockProduit stock = stock(produit, rayon, 42);
        viderLeCache();

        StockProduitDTO lu = services.stockProduitService.getStockProduit(stock.getId());

        assertEquals(42, lu.getQtyStock());
        assertEquals(STORAGE_RAYON_ID, lu.getStorageId());
        assertEquals(produit.getId(), lu.getProduitId());
        assertEquals(StorageType.PRINCIPAL, lu.getType());
    }

    @Test
    @DisplayName("La recherche pour répartition retrouve le produit par son libellé")
    void rechercheParLibelle() {
        Produit produit = produit("CETIRIZINE MYLAN");
        fournisseurProduit(produit);
        stock(produit, rayon, 20);
        Produit autre = produit("LORATADINE BIOGARAN");
        stock(autre, rayon, 20);
        viderLeCache();

        List<StockProduitSearchDTO> resultats = services.stockProduitService.searchStockProduitsForRepartition(
            STORAGE_RAYON_ID,
            "cetirizine"
        );

        assertEquals(1, resultats.size(), "la recherche est insensible à la casse");
        assertEquals(produit.getId(), resultats.getFirst().getProduitId());
        assertEquals(20, resultats.getFirst().getQtyStock());
    }

    @Test
    @DisplayName("La recherche retrouve aussi le produit par son code CIP")
    void rechercheParCodeCip() {
        Produit produit = produit("MONTELUKAST");
        String codeCip = fournisseurProduit(produit).getCodeCip();
        stock(produit, rayon, 15);
        viderLeCache();

        List<StockProduitSearchDTO> resultats = services.stockProduitService.searchStockProduitsForRepartition(
            STORAGE_RAYON_ID,
            codeCip
        );

        assertEquals(1, resultats.size());
        assertEquals(codeCip, resultats.getFirst().getProduitCodeCip());
    }

    @Test
    @DisplayName("Un produit sans stock sur l'emplacement sort de la recherche")
    void rechercheIgnoreLeStockNul() {
        Produit produit = produit("EPUISE VO");
        fournisseurProduit(produit);
        stock(produit, rayon, 0);
        viderLeCache();

        List<StockProduitSearchDTO> resultats = services.stockProduitService.searchStockProduitsForRepartition(
            STORAGE_RAYON_ID,
            "EPUISE"
        );

        assertTrue(resultats.isEmpty(), "la répartition ne propose que ce qui est réellement en rayon");
    }

    @Test
    @DisplayName("La recherche remonte tous les emplacements du produit trouvé")
    void rechercheRemonteTousLesStocks() {
        Produit produit = produit("SOLUPRED");
        fournisseurProduit(produit);
        stock(produit, rayon, 18);
        stock(produit, reserve, 60);
        viderLeCache();

        List<StockProduitSearchDTO> resultats = services.stockProduitService.searchStockProduitsForRepartition(
            STORAGE_RAYON_ID,
            "SOLUPRED"
        );

        assertEquals(1, resultats.size(), "le produit ne remonte qu'une fois, sur l'emplacement demandé");
        assertEquals(2, resultats.getFirst().getAllStocks().size(), "mais il porte la vue de tous ses emplacements");
        assertEquals(
            78,
            resultats.getFirst().getAllStocks().stream().mapToInt(StockProduitDTO::getQtyStock).sum()
        );
    }
}
