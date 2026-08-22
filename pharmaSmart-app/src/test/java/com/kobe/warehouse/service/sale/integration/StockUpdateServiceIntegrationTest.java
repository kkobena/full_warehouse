package com.kobe.warehouse.service.sale.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.domain.enumeration.TypeMagasin;
import com.kobe.warehouse.service.sale.impl.StockUpdateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link StockUpdateService} sur un vrai PostgreSQL.
 *
 * <p>Le stock d'un produit tient dans deux compartiments distincts : {@code qty_stock} et
 * {@code qty_ug} pour les unités gratuites. Une vente puise dans les deux, une annulation les rend
 * séparément, et {@code qty_ug} porte une contrainte {@code >= 0} en base. Vérifier ces mouvements
 * ailleurs qu'en base laisserait passer un compartiment qui part en négatif.
 */
@DisplayName("StockUpdateService — mouvements de stock sur PostgreSQL")
class StockUpdateServiceIntegrationTest extends AbstractSaleIntegrationTest {

    @Test
    @DisplayName("Une vente débite le rayon et rend le stock d'avant et d'après")
    void debitDuRayon() {
        Produit produit = produitEnStock("STOCK A", 1_000, 600, 0, 20);
        StockProduit stock = stockRayon(produit);
        SalesLine ligne = ligneVendue(produit, 5, 0);

        StockUpdateService.StockUpdateResult resultat = services.stockUpdateService.updateStock(ligne, stock);
        viderLeCache();

        assertEquals(20, resultat.quantityBefore());
        assertEquals(15, resultat.quantityAfter());
        StockProduit relu = em.find(StockProduit.class, stock.getId());
        assertEquals(15, relu.getQtyStock());
        assertEquals(15, relu.getQtyVirtual(), "le stock virtuel suit le stock réel");
        verify(services.suggestionReassortService).createRayonSuggestionReassort(relu);
    }

    @Test
    @DisplayName("Les unités gratuites sortent de leur propre compartiment")
    void debitDesUnitesGratuites() {
        Produit produit = produit("STOCK UG", 2_000, 1_200, 0);
        StockProduit stock = stock(produit, rayon, 10, 4);
        em.flush();
        SalesLine ligne = ligneVendue(produit, 6, 4);

        StockUpdateService.StockUpdateResult resultat = services.stockUpdateService.updateStock(ligne, stock);
        viderLeCache();

        assertEquals(14, resultat.quantityBefore(), "10 payantes + 4 gratuites");
        StockProduit relu = em.find(StockProduit.class, stock.getId());
        assertEquals(8, relu.getQtyStock(), "10 − (6 servies − 4 gratuites)");
        assertEquals(0, relu.getQtyUG());
    }

    @Test
    @DisplayName("L'annulation rend chaque compartiment à son niveau d'origine")
    void restitutionApresAnnulation() {
        Produit produit = produit("STOCK ANNUL", 1_500, 900, 0);
        StockProduit stock = stock(produit, rayon, 10, 3);
        em.flush();

        SalesLine vendue = ligneVendue(produit, 5, 3);
        services.stockUpdateService.updateStock(vendue, stock);
        em.flush();
        assertEquals(8, em.find(StockProduit.class, stock.getId()).getQtyStock());

        SalesLine contrepassation = ligneVendue(produit, -5, -3);
        services.stockUpdateService.updateStockOnCancellation(contrepassation, stock);
        viderLeCache();

        StockProduit relu = em.find(StockProduit.class, stock.getId());
        assertEquals(10, relu.getQtyStock(), "les quantités négatives de la contrepassation restaurent le rayon");
        assertEquals(3, relu.getQtyUG());
        verify(services.suggestionReassortService).createReserveSuggestionReassort(relu);
    }

    @Test
    @DisplayName("Une vente dépôt crée la fiche de stock manquante côté dépôt")
    void creationDuStockDepot() {
        Produit produit = produitEnStock("STOCK DEPOT", 3_000, 1_800, 0, 30);
        Storage stockageDepot = depot("DEPOT STOCK").getPrimaryStorage();
        SalesLine ligne = ligneVendue(produit, 7, 0);

        StockUpdateService.StockUpdateResult resultat = services.stockUpdateService.updateStockDepot(ligne, stockageDepot);
        viderLeCache();

        assertEquals(0, resultat.quantityBefore(), "le dépôt ne connaissait pas encore ce produit");
        assertEquals(7, resultat.quantityAfter());
        StockProduit cree = services.stockProduitRepository.findOneByProduitIdAndStockageId(produit.getId(), stockageDepot.getId());
        assertNotNull(cree);
        assertEquals(7, cree.getQtyStock());
    }

    @Test
    @DisplayName("Une seconde vente dépôt s'ajoute au stock déjà présent")
    void cumulDuStockDepot() {
        Produit produit = produitEnStock("STOCK DEPOT 2", 3_000, 1_800, 0, 30);
        Storage stockageDepot = depot("DEPOT CUMUL").getPrimaryStorage();
        services.stockUpdateService.updateStockDepot(ligneVendue(produit, 4, 0), stockageDepot);
        em.flush();

        StockUpdateService.StockUpdateResult resultat = services.stockUpdateService.updateStockDepot(
            ligneVendue(produit, 3, 0),
            stockageDepot
        );
        viderLeCache();

        assertEquals(4, resultat.quantityBefore());
        assertEquals(7, resultat.quantityAfter());
    }

    // ===== outils =====

    private StockProduit stockRayon(Produit produit) {
        return services.stockProduitRepository.findOneByProduitIdAndStockageId(produit.getId(), STORAGE_RAYON_ID);
    }

    /** Une ligne de vente déjà enregistrée : le service de stock a besoin de son identifiant. */
    private SalesLine ligneVendue(Produit produit, int quantite, int quantiteUg) {
        CashSale vente = venteFermee(produit, Math.abs(quantite), Math.abs(quantite));
        SalesLine ligne = vente.getSalesLines().iterator().next();
        ligne.setQuantityRequested(quantite);
        ligne.setQuantitySold(quantite);
        ligne.setQuantityUg(quantiteUg);
        // Une contrepassation porte des quantités négatives : son montant déclarable suit, sinon la
        // contrainte sales_line_declarable_ck refuse la ligne.
        ligne.setSalesAmount(quantite * produit.getRegularUnitPrice());
        ligne.setAmountToBeTakenIntoAccount(quantite * produit.getRegularUnitPrice());
        return ligne;
    }

    private Magasin depot(String nom) {
        Magasin depot = new Magasin();
        depot.setName(nom);
        depot.setFullName(nom);
        depot.setTypeMagasin(TypeMagasin.DEPOT);
        em.persist(depot);

        Storage stockage = new Storage();
        stockage.setName("Stock " + nom);
        stockage.setStorageType(StorageType.PRINCIPAL);
        stockage.setMagasin(depot);
        em.persist(stockage);

        depot.setPrimaryStorage(stockage);
        em.flush();
        return depot;
    }
}
