package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.errors.DeconditionnementStockOut;
import com.kobe.warehouse.service.errors.StockException;
import com.kobe.warehouse.service.id_generator.SaleLineIdGeneratorService;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.reassort.RepartitionStockService;
import com.kobe.warehouse.service.stock.LotService;
import com.kobe.warehouse.service.stock.LotStockLocationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Base implementation for SalesLineService.
 * This class consolidates the common logic previously duplicated in
 */
@Service
@Transactional
public class SalesLineServiceBaseImpl extends SalesLineServiceImpl {

    public SalesLineServiceBaseImpl(
        ProduitRepository produitRepository,
        SalesLineRepository salesLineRepository,
        StockProduitRepository stockProduitRepository,
        LotService lotService,
        InventoryTransactionService inventoryTransactionService,
        SaleLineIdGeneratorService saleLineIdGeneratorService,
        StockUpdateService stockUpdateService,
        StorageService storageService,
        RepartitionStockService repartitionStockService,
        LotStockLocationService lotStockLocationService
    ) {
        super(
            produitRepository,
            salesLineRepository,
            stockProduitRepository,
            lotService,
            inventoryTransactionService,
            saleLineIdGeneratorService,
            stockUpdateService,
            storageService,
            repartitionStockService,
            lotStockLocationService
        );
    }

    @Override
    public SalesLine createSaleLineFromDTO(SaleLineDTO dto, Integer stockageId) throws StockException, DeconditionnementStockOut {
        return super.setCommonSaleLine(dto, stockageId);
    }

    @Override
    public void saveAllSalesLines(Set<SalesLine> salesLines, AppUser user, Integer storageId) {
        super.save(salesLines, user, storageId);
    }

    @Override
    public List<SalesLine> createSaleLinesFromDTO(CashSale cashSale, List<SaleLineDTO> saleLines, Integer stockageId) {
        return saleLines.stream().map(saleLineDTO -> {
            SalesLine salesLine = super.setCommonSaleLine(saleLineDTO, stockageId);
            salesLine.setSales(cashSale);
            return salesLine;
        }).toList();
    }
}
