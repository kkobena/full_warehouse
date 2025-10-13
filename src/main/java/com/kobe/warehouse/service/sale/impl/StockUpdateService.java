package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.LogsService;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for stock update operations.
 * This service consolidates stock update logic previously duplicated across
 * SalesLineServiceImpl's createInventory() and save() methods.
 */
@Service
@Transactional
public class StockUpdateService {

    private final StockProduitRepository stockProduitRepository;
    private final LogsService logsService;

    public StockUpdateService(StockProduitRepository stockProduitRepository, LogsService logsService) {
        this.stockProduitRepository = stockProduitRepository;
        this.logsService = logsService;
    }

    /**
     * Updates stock for a sales line and returns quantity information.
     *
     * @param salesLine the sales line containing product and quantity information
     * @param storageId the storage location ID
     * @return StockUpdateResult containing quantity before and after the update
     */
    public StockUpdateResult updateStock(SalesLine salesLine, Long storageId) {
        Produit produit = salesLine.getProduit();
        StockProduit stockProduit = stockProduitRepository.findOneByProduitIdAndStockageId(produit.getId(), storageId);

        int quantityBefore = stockProduit.getTotalStockQuantity();
        int quantityAfter = quantityBefore - salesLine.getQuantityRequested();

        // Log force stock if needed
        if (quantityBefore < salesLine.getQuantityRequested()) {
            logsService.create(
                TransactionType.FORCE_STOCK,
                TransactionType.FORCE_STOCK.getValue(),
                salesLine.getId().getId().toString()
            );
        }

        // Log price modification if applicable
        logPriceModification(salesLine, produit);

        // Update stock quantities
        stockProduit.setQtyStock(stockProduit.getQtyStock() - (salesLine.getQuantityRequested() - salesLine.getQuantityUg()));
        stockProduit.setQtyUG(stockProduit.getQtyUG() - salesLine.getQuantityUg());
        stockProduit.setUpdatedAt(LocalDateTime.now());
        stockProduitRepository.save(stockProduit);

        return new StockUpdateResult(quantityBefore, quantityAfter);
    }

    /**
     * Logs price modification if the sale price differs from the usual price.
     */
    private void logPriceModification(SalesLine salesLine, Produit produit) {
        FournisseurProduit fournisseurProduitPrincipal = produit.getFournisseurProduitPrincipal();
        if (fournisseurProduitPrincipal != null && fournisseurProduitPrincipal.getPrixUni() < salesLine.getRegularUnitPrice()) {
            String description = String.format(
                "Le prix de vente du produit %s %s a été modifié sur la vente %s prix usuel:  %d prix sur la vente %d",
                fournisseurProduitPrincipal.getCodeCip(),
                produit.getLibelle(),
                salesLine.getSales().getNumberTransaction(),
                fournisseurProduitPrincipal.getPrixUni(),
                salesLine.getRegularUnitPrice()
            );
            logsService.create(
                TransactionType.MODIFICATION_PRIX_PRODUCT_A_LA_VENTE,
                description,
                salesLine.getId().getId().toString()
            );
        }
    }

    /**
     * Result of a stock update operation containing quantity information.
     */
    public static class StockUpdateResult {

        private final int quantityBefore;
        private final int quantityAfter;

        public StockUpdateResult(int quantityBefore, int quantityAfter) {
            this.quantityBefore = quantityBefore;
            this.quantityAfter = quantityAfter;
        }

        public int getQuantityBefore() {
            return quantityBefore;
        }

        public int getQuantityAfter() {
            return quantityAfter;
        }
    }
}
