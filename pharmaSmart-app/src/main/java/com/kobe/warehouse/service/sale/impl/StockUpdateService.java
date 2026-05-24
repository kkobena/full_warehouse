package com.kobe.warehouse.service.sale.impl;

import static java.util.Objects.isNull;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.LogsService;
import java.time.LocalDateTime;

import com.kobe.warehouse.service.reassort.SuggestionReassortService;
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
    private final SuggestionReassortService suggestionReassortService;

    public StockUpdateService(StockProduitRepository stockProduitRepository, LogsService logsService, SuggestionReassortService suggestionReassortService) {
        this.stockProduitRepository = stockProduitRepository;
        this.logsService = logsService;
        this.suggestionReassortService = suggestionReassortService;
    }

    /**
     * Updates stock for a sales line and returns quantity information.
     *
     * @param salesLine the sales line containing product and quantity information
     * @param stockProduit the stock product to be updated
     * @return StockUpdateResult containing quantity before and after the update
     */
    public StockUpdateResult updateStock(SalesLine salesLine,StockProduit stockProduit) {
        Produit produit = salesLine.getProduit();


        int quantityBefore = stockProduit.getTotalStockQuantity();
        int quantityAfter = quantityBefore - salesLine.getQuantityRequested();

        // Log force stock if needed
        if (quantityBefore < salesLine.getQuantityRequested()) {
            logsService.create(TransactionType.FORCE_STOCK, TransactionType.FORCE_STOCK.getValue(), salesLine.getId().getId().toString());
        }

        // Log price modification if applicable
        logPriceModification(salesLine, produit);

        // Update stock quantities
        stockProduit.setQtyStock(stockProduit.getQtyStock() - (salesLine.getQuantityRequested() - salesLine.getQuantityUg()));
        stockProduit.setQtyUG(stockProduit.getQtyUG() - salesLine.getQuantityUg());
        stockProduit.setUpdatedAt(LocalDateTime.now());
        stockProduit.setQtyVirtual(stockProduit.getQtyStock());
        stockProduitRepository.save(stockProduit);
        suggestionReassortService.createRayonSuggestionReassort(stockProduit);

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
            logsService.create(TransactionType.MODIFICATION_PRIX_PRODUCT_A_LA_VENTE, description, salesLine.getId().getId().toString());
        }
    }

    /**
     * Restaure le stock rayon après annulation d'une vente (quantités négatives dans canceledLine)
     * et crée une suggestion de réassort réserve si le rayon devient excédentaire (rayon > stockMaxi).
     *
     * @param canceledLine ligne d'annulation avec quantités négatives
     * @param stockProduit StockProduit rayon à restaurer
     */
    public void updateStockOnCancellation(SalesLine canceledLine, StockProduit stockProduit) {
        // canceledLine a des quantités négatives → la soustraction restaure le stock.
        // Symétrie avec updateStock : qtyStock ne contient pas les UG, on restaure chaque
        // compartiment séparément pour éviter de gonfler qtyStock des UG annulées.
        stockProduit.setQtyStock(stockProduit.getQtyStock() - (canceledLine.getQuantityRequested() - canceledLine.getQuantityUg()));
        stockProduit.setQtyUG(stockProduit.getQtyUG() - canceledLine.getQuantityUg());
        stockProduit.setUpdatedAt(LocalDateTime.now());
        stockProduitRepository.save(stockProduit);
        // Si le rayon est maintenant en excédent par rapport à stockMaxi et que la réserve
        // est sous seuilMini → suggérer un transfert rayon→réserve au pharmacien
        suggestionReassortService.createReserveSuggestionReassort(stockProduit);
    }

    public StockUpdateResult updateStockDepot(SalesLine salesLine, Storage storage) {
        Produit produit = salesLine.getProduit();
        StockProduit stockProduit = stockProduitRepository.findOneByProduitIdAndStockageId(produit.getId(), storage.getId());
        int quantityBefore = 0;
        if (isNull(stockProduit)) {
            stockProduit = createStockProduitIfAbsent(produit, storage);
        } else {
            quantityBefore = stockProduit.getTotalStockQuantity();
        }

        int quantityAfter = quantityBefore + salesLine.getQuantityRequested();

        // Update stock quantities
        stockProduit.setQtyStock(quantityAfter);
        stockProduit.setQtyVirtual(stockProduit.getQtyStock());
        stockProduitRepository.save(stockProduit);

        return new StockUpdateResult(quantityBefore, quantityAfter);
    }

    private StockProduit createStockProduitIfAbsent(Produit produit, Storage storage) {
        StockProduit newStockProduit = new StockProduit();
        newStockProduit.setProduit(produit);
        newStockProduit.setStorage(storage);
        newStockProduit.setQtyStock(0);
        newStockProduit.setQtyUG(0);
        newStockProduit.setQtyVirtual(0);
        newStockProduit.setCreatedAt(LocalDateTime.now());
        newStockProduit.setUpdatedAt(newStockProduit.getCreatedAt());
        return newStockProduit;
    }

    /**
         * Result of a stock update operation containing quantity information.
         */
        public record StockUpdateResult(int quantityBefore, int quantityAfter) {

    }
}
