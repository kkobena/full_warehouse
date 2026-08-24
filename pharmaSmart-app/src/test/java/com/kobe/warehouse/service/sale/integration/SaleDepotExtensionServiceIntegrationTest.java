package com.kobe.warehouse.service.sale.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;

import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.VenteDepot;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.domain.enumeration.TypeMagasin;
import com.kobe.warehouse.domain.enumeration.TypePrescription;
import com.kobe.warehouse.service.dto.DepotExtensionSaleDTO;
import com.kobe.warehouse.service.dto.MagasinDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.errors.GenericError;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link com.kobe.warehouse.service.sale.SaleDepotExtensionService} sur un vrai PostgreSQL.
 *
 * <p>Une vente dépôt n'encaisse rien : elle déplace du stock d'une officine vers un dépôt
 * d'extension. Sa clôture écrit donc deux fois — débit du rayon, crédit du stockage du dépôt — et
 * crée au besoin la fiche de stock manquante côté dépôt. C'est ce double mouvement, et le fait que
 * la vente porte la catégorie {@code CA_DEPOT} qui l'exclut du chiffre d'affaires officine, que ces
 * tests vérifient sur la base.
 */
@DisplayName("SaleDepotExtensionService — ventes dépôt sur PostgreSQL")
class SaleDepotExtensionServiceIntegrationTest extends AbstractSaleIntegrationTest {

    @Test
    @DisplayName("La vente dépôt est écrite avec son dépôt et la catégorie CA_DEPOT")
    void creationVenteDepot() {
        Magasin depot = depot("DEPOT NORD");
        Produit produit = produitEnStock("PARACETAMOL 500", 1_000, 600, 0, 100);

        DepotExtensionSaleDTO cree = services.saleDepotExtensionService.create(venteDe(depot, produit, 10));
        viderLeCache();

        VenteDepot vente = em.find(VenteDepot.class, cree.getSaleId());
        assertNotNull(vente);
        assertEquals(CategorieChiffreAffaire.CA_DEPOT, vente.getCategorieChiffreAffaire(), "hors chiffre d'affaires officine");
        assertEquals(depot.getId(), vente.getDepot().getId());
        assertEquals(10_000, vente.getSalesAmount());
        assertEquals(10_000, vente.getAmountToBePaid());
        assertEquals(1, compter("SELECT count(*) FROM sales_line WHERE sales_id = " + cree.getSaleId().getId()));
    }

    @Test
    @DisplayName("La clôture débite le rayon et crédite le stockage du dépôt")
    void clotureDeplaceLeStock() {
        Magasin depot = depot("DEPOT SUD");
        Produit produit = produitEnStock("AMOXI 500", 800, 500, 0, 100);
        DepotExtensionSaleDTO cree = services.saleDepotExtensionService.create(venteDe(depot, produit, 30));
        // La clôture est une requête distincte : la vente est relue depuis la base, avec son
        // dépôt complet — et non l'ébauche à un seul identifiant que la création lui a posée.
        viderLeCache();

        services.saleDepotExtensionService.save(clotureDe(cree.getSaleId()));
        viderLeCache();

        VenteDepot vente = em.find(VenteDepot.class, cree.getSaleId());
        assertEquals(SalesStatut.CLOSED, vente.getStatut());
        assertNotNull(vente.getNumberTransaction());
        assertEquals(70, stock(produit, STORAGE_RAYON_ID).getQtyStock(), "le rayon de l'officine est débité");
        StockProduit stockDepot = stock(produit, depot.getPrimaryStorage().getId());
        assertNotNull(stockDepot, "la fiche de stock du dépôt est créée à la volée");
        assertEquals(30, stockDepot.getQtyStock());
        verify(services.inventoryTransactionService).saveVenteDepotExtensionInventoryTransactions(any(Magasin.class), anyList());
    }

    @Test
    @DisplayName("Une vente dépôt sans dépôt ne peut pas être clôturée")
    void clotureSansDepot() {
        Magasin depot = depot("DEPOT EST");
        Produit produit = produitEnStock("IBU 400", 700, 400, 0, 50);
        DepotExtensionSaleDTO cree = services.saleDepotExtensionService.create(venteDe(depot, produit, 5));
        viderLeCache();
        em.createNativeQuery("UPDATE sales SET depot_id = NULL WHERE id = " + cree.getSaleId().getId()).executeUpdate();
        viderLeCache();

        DepotExtensionSaleDTO cloture = clotureDe(cree.getSaleId());
        assertThrows(GenericError.class, () -> services.saleDepotExtensionService.save(cloture));
    }

    @Test
    @DisplayName("Changer de dépôt réécrit la vente en base")
    void changementDeDepot() {
        Magasin premier = depot("DEPOT A");
        Magasin second = depot("DEPOT B");
        Produit produit = produitEnStock("VITAMINE D", 1_500, 900, 0, 50);
        DepotExtensionSaleDTO cree = services.saleDepotExtensionService.create(venteDe(premier, produit, 4));

        services.saleDepotExtensionService.changeDepot(cree.getSaleId(), second.getId());
        viderLeCache();

        assertEquals(second.getId(), em.find(VenteDepot.class, cree.getSaleId()).getDepot().getId());
    }

    @Test
    @DisplayName("Ajouter une ligne recalcule le montant de la vente dépôt")
    void ajoutDeLigne() {
        Magasin depot = depot("DEPOT OUEST");
        Produit premier = produitEnStock("SIROP A", 2_000, 1_200, 0, 50);
        Produit second = produitEnStock("SIROP B", 3_000, 1_800, 0, 50);
        DepotExtensionSaleDTO cree = services.saleDepotExtensionService.create(venteDe(depot, premier, 2));

        SaleLineDTO ajout = ligneDe(second, 3);
        ajout.setSaleCompositeId(cree.getSaleId());
        services.saleDepotExtensionService.addOrUpdateSaleLine(ajout);
        viderLeCache();

        VenteDepot vente = em.find(VenteDepot.class, cree.getSaleId());
        assertEquals(2, vente.getSalesLines().size());
        assertEquals(13_000, vente.getSalesAmount(), "2 × 2 000 + 3 × 3 000");
        assertEquals(13_000, vente.getAmountToBePaid());
    }

    @Test
    @DisplayName("Modifier la quantité d'une ligne met la vente à jour")
    void modificationDeQuantite() {
        Magasin depot = depot("DEPOT CENTRE");
        Produit produit = produitEnStock("COLLYRE", 2_500, 1_500, 0, 50);
        DepotExtensionSaleDTO cree = services.saleDepotExtensionService.create(venteDe(depot, produit, 2));
        viderLeCache();
        VenteDepot vente = em.find(VenteDepot.class, cree.getSaleId());

        SaleLineDTO dto = ligneDe(produit, 6);
        dto.setSaleCompositeId(cree.getSaleId());
        dto.setSaleLineId(vente.getSalesLines().iterator().next().getId());
        services.saleDepotExtensionService.updateItemQuantityRequested(dto, false);
        viderLeCache();

        assertEquals(15_000, em.find(VenteDepot.class, cree.getSaleId()).getSalesAmount());
    }

    @Test
    @DisplayName("L'annulation d'une vente dépôt écrit sa contrepassation")
    void annulationDUneVenteDepot() {
        Magasin depot = depot("DEPOT ANNULATION");
        Produit produit = produitEnStock("SIROP ANNULE", 1_000, 600, 0, 100);
        DepotExtensionSaleDTO cree = services.saleDepotExtensionService.create(venteDe(depot, produit, 8));
        viderLeCache();
        services.saleDepotExtensionService.save(clotureDe(cree.getSaleId()));
        viderLeCache();

        services.saleDepotExtensionService.cancel(cree.getSaleId());
        viderLeCache();

        assertTrue(em.find(VenteDepot.class, cree.getSaleId()).isCanceled());
        assertEquals(
            -8_000,
            compter("SELECT sales_amount FROM sales WHERE canceled_sale_id = " + cree.getSaleId().getId()),
            "la contrepassation dépôt passe elle aussi la contrainte sur le montant déclarable"
        );
    }

    @Test
    @DisplayName("Supprimer une prévente dépôt efface la vente et ses lignes")
    void suppressionDUnePrevente() {
        Magasin depot = depot("DEPOT PREVENTE");
        Produit produit = produitEnStock("BANDE", 400, 200, 0, 50);
        DepotExtensionSaleDTO cree = services.saleDepotExtensionService.create(venteDe(depot, produit, 3));
        viderLeCache();

        services.saleDepotExtensionService.deleteSalePrevente(cree.getSaleId());
        viderLeCache();

        assertEquals(0, compter("SELECT count(*) FROM sales WHERE id = " + cree.getSaleId().getId()));
        assertEquals(0, compter("SELECT count(*) FROM sales_line WHERE sales_id = " + cree.getSaleId().getId()));
    }

    // ===== outils =====

    private StockProduit stock(Produit produit, Integer storageId) {
        return services.stockProduitRepository.findOneByProduitIdAndStockageId(produit.getId(), storageId);
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

    private DepotExtensionSaleDTO clotureDe(SaleId saleId) {
        DepotExtensionSaleDTO dto = new DepotExtensionSaleDTO();
        dto.setSaleId(saleId);
        dto.setPayrollAmount(0);
        dto.setRestToPay(0);
        return dto;
    }

    private DepotExtensionSaleDTO venteDe(Magasin depot, Produit produit, int quantite) {
        DepotExtensionSaleDTO dto = new DepotExtensionSaleDTO();
        dto.setNatureVente(NatureVente.ASSURANCE);
        dto.setTypePrescription(TypePrescription.PRESCRIPTION);
        MagasinDTO magasinDepot = new MagasinDTO();
        magasinDepot.setId(depot.getId());
        dto.setMagasin(magasinDepot);
        dto.setSalesLines(List.of(ligneDe(produit, quantite)));
        return dto;
    }

    private SaleLineDTO ligneDe(Produit produit, int quantite) {
        SaleLineDTO ligne = new SaleLineDTO();
        ligne.setProduitId(produit.getId());
        ligne.setQuantityRequested(quantite);
        ligne.setQuantitySold(quantite);
        ligne.setRegularUnitPrice(produit.getRegularUnitPrice());
        return ligne;
    }
}
