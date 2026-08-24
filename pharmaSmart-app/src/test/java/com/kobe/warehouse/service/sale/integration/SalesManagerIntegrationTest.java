package com.kobe.warehouse.service.sale.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.VenteDepot;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.domain.enumeration.TypeMagasin;
import com.kobe.warehouse.domain.enumeration.TypePrescription;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.DepotExtensionSaleDTO;
import com.kobe.warehouse.service.dto.MagasinDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.errors.StockException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link com.kobe.warehouse.service.sale.SalesManager} sur un vrai PostgreSQL.
 *
 * <p>Le gestionnaire est l'aiguillage : il modifie une ligne, puis renvoie la vente à son propre
 * service pour recalcul — comptant, assurance ou dépôt, ce n'est pas le même calcul ni le même
 * repository. C'est aussi lui qui garantit l'invariant que la contrainte
 * {@code sales_line_declarable_ck} surveille : après chaque modification, le montant déclarable de
 * la ligne est réétabli avant que quoi que ce soit ne parte en base.
 */
@DisplayName("SalesManager — aiguillage des modifications de ligne sur PostgreSQL")
class SalesManagerIntegrationTest extends AbstractSaleIntegrationTest {

    @Test
    @DisplayName("Sur une vente comptant, l'ajout d'une ligne recalcule et enregistre la vente")
    void ajoutSurVenteComptant() {
        Produit premier = produitEnStock("MANAGER A", 1_000, 600, 0, 30);
        Produit second = produitEnStock("MANAGER B", 2_000, 1_200, 0, 30);
        CashSaleDTO vente = services.saleService.createCashSale(venteComptant(premier, 1));
        viderLeCache();
        CashSale entite = em.find(CashSale.class, vente.getSaleId());

        SaleLineDTO ajout = ligneDe(second, 2);
        ajout.setSaleCompositeId(vente.getSaleId());
        services.salesManager.addOrUpdateSaleLine(ajout, entite);
        viderLeCache();

        CashSale relue = em.find(CashSale.class, vente.getSaleId());
        assertEquals(2, relue.getSalesLines().size());
        assertEquals(5_000, relue.getSalesAmount(), "1 000 + 2 × 2 000");
        assertEquals(5_000, relue.getAmountToBePaid());
    }

    @Test
    @DisplayName("Modifier la quantité réétablit le montant déclarable avant écriture")
    void modificationDeQuantite() {
        Produit produit = produitEnStock("MANAGER QTE", 3_000, 1_800, 0, 30);
        CashSaleDTO vente = services.saleService.createCashSale(venteComptant(produit, 4));
        viderLeCache();
        CashSale entite = em.find(CashSale.class, vente.getSaleId());
        SalesLine ligne = entite.getSalesLines().iterator().next();

        SaleLineDTO baisse = ligneDe(produit, 1);
        baisse.setSaleCompositeId(vente.getSaleId());
        baisse.setSaleLineId(ligne.getId());
        services.salesManager.updateItemQuantityRequested(baisse, entite, false);
        viderLeCache();

        SalesLine relue = em.find(SalesLine.class, ligne.getId());
        assertEquals(1, relue.getQuantityRequested());
        assertEquals(3_000, relue.getSalesAmount());
        assertEquals(3_000, relue.getAmountToBeTakenIntoAccount(), "sinon la contrainte déclarable refuserait la ligne");
        assertEquals(3_000, em.find(CashSale.class, vente.getSaleId()).getSalesAmount());
    }

    @Test
    @DisplayName("Incrémenter cumule sur la quantité déjà saisie")
    void incrementDeQuantite() {
        Produit produit = produitEnStock("MANAGER INC", 1_000, 600, 0, 30);
        CashSaleDTO vente = services.saleService.createCashSale(venteComptant(produit, 2));
        viderLeCache();
        CashSale entite = em.find(CashSale.class, vente.getSaleId());
        SalesLine ligne = entite.getSalesLines().iterator().next();

        SaleLineDTO increment = ligneDe(produit, 3);
        increment.setSaleCompositeId(vente.getSaleId());
        increment.setSaleLineId(ligne.getId());
        services.salesManager.updateItemQuantityRequested(increment, entite, true);
        viderLeCache();

        assertEquals(5, em.find(SalesLine.class, ligne.getId()).getQuantityRequested());
        assertEquals(5_000, em.find(CashSale.class, vente.getSaleId()).getSalesAmount());
    }

    @Test
    @DisplayName("Une demande au-delà du stock est refusée avant toute écriture")
    void demandeAuDelaDuStock() {
        Produit produit = produitEnStock("MANAGER STOCK", 1_000, 600, 0, 3);
        CashSaleDTO vente = services.saleService.createCashSale(venteComptant(produit, 1));
        viderLeCache();
        CashSale entite = em.find(CashSale.class, vente.getSaleId());
        SalesLine ligne = entite.getSalesLines().iterator().next();

        SaleLineDTO trop = ligneDe(produit, 10);
        trop.setSaleCompositeId(vente.getSaleId());
        trop.setSaleLineId(ligne.getId());

        assertThrows(StockException.class, () -> services.salesManager.updateItemQuantityRequested(trop, entite, false));
    }

    @Test
    @DisplayName("Supprimer une ligne défalque son montant de la vente comptant")
    void suppressionSurVenteComptant() {
        Produit premier = produitEnStock("MANAGER DEL A", 1_000, 600, 0, 30);
        Produit second = produitEnStock("MANAGER DEL B", 4_000, 2_400, 0, 30);
        CashSaleDTO vente = services.saleService.createCashSale(venteComptant(premier, 1));
        SaleLineDTO ajout = ligneDe(second, 1);
        ajout.setSaleCompositeId(vente.getSaleId());
        services.saleService.addOrUpdateSaleLine(ajout);
        viderLeCache();

        CashSale entite = em.find(CashSale.class, vente.getSaleId());
        SalesLine aSupprimer = entite
            .getSalesLines()
            .stream()
            .filter(l -> l.getProduit().getId().equals(second.getId()))
            .findFirst()
            .orElseThrow();
        services.salesManager.deleteSaleLineById(aSupprimer);
        viderLeCache();

        assertEquals(1, compter("SELECT count(*) FROM sales_line WHERE sales_id = " + vente.getSaleId().getId()));
        assertEquals(1_000, em.find(CashSale.class, vente.getSaleId()).getSalesAmount());
    }

    @Test
    @DisplayName("Sur une vente dépôt, l'aiguillage passe par le repository des ventes dépôt")
    void modificationSurVenteDepot() {
        Magasin depot = depot("DEPOT MANAGER");
        Produit produit = produitEnStock("MANAGER DEPOT", 2_500, 1_500, 0, 50);
        DepotExtensionSaleDTO vente = services.saleDepotExtensionService.create(venteDepot(depot, produit, 2));
        viderLeCache();
        VenteDepot entite = em.find(VenteDepot.class, vente.getSaleId());
        SalesLine ligne = entite.getSalesLines().iterator().next();

        SaleLineDTO hausse = ligneDe(produit, 6);
        hausse.setSaleCompositeId(vente.getSaleId());
        hausse.setSaleLineId(ligne.getId());
        services.salesManager.updateItemQuantityRequested(hausse, entite, false);
        viderLeCache();

        VenteDepot relue = em.find(VenteDepot.class, vente.getSaleId());
        assertEquals(15_000, relue.getSalesAmount());
        assertEquals(15_000, relue.getAmountToBePaid());
        assertEquals(0, relue.getAmountToBeTakenIntoAccount(), "une vente dépôt ne compte pas dans le chiffre d'affaires");
    }

    @Test
    @DisplayName("Supprimer la ligne d'une vente dépôt met à jour la vente dépôt")
    void suppressionSurVenteDepot() {
        Magasin depot = depot("DEPOT MANAGER DEL");
        Produit premier = produitEnStock("DEPOT DEL A", 1_000, 600, 0, 50);
        Produit second = produitEnStock("DEPOT DEL B", 3_000, 1_800, 0, 50);
        DepotExtensionSaleDTO vente = services.saleDepotExtensionService.create(venteDepot(depot, premier, 2));
        SaleLineDTO ajout = ligneDe(second, 1);
        ajout.setSaleCompositeId(vente.getSaleId());
        services.saleDepotExtensionService.addOrUpdateSaleLine(ajout);
        viderLeCache();

        VenteDepot entite = em.find(VenteDepot.class, vente.getSaleId());
        SalesLine aSupprimer = entite
            .getSalesLines()
            .stream()
            .filter(l -> l.getProduit().getId().equals(second.getId()))
            .findFirst()
            .orElseThrow();
        services.salesManager.deleteSaleLineById(aSupprimer);
        viderLeCache();

        assertEquals(1, compter("SELECT count(*) FROM sales_line WHERE sales_id = " + vente.getSaleId().getId()));
        assertEquals(2_000, em.find(VenteDepot.class, vente.getSaleId()).getSalesAmount());
    }

    // ===== outils =====

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

    private CashSaleDTO venteComptant(Produit produit, int quantite) {
        CashSaleDTO dto = new CashSaleDTO();
        dto.setNatureVente(NatureVente.COMPTANT);
        dto.setTypePrescription(TypePrescription.PRESCRIPTION);
        dto.setSalesLines(List.of(ligneDe(produit, quantite)));
        return dto;
    }

    private DepotExtensionSaleDTO venteDepot(Magasin depot, Produit produit, int quantite) {
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
