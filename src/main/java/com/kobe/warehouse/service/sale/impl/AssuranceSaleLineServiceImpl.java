package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.config.IdGeneratorService;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.repository.InventoryTransactionRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.stock.LotService;
import com.kobe.warehouse.service.stock.SuggestionProduitService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AssuranceSaleLineServiceImpl extends SalesLineServiceImpl {

    public AssuranceSaleLineServiceImpl(
        ProduitRepository produitRepository,
        SalesLineRepository salesLineRepository,
        StockProduitRepository stockProduitRepository,
        LogsService logsService,
        SuggestionProduitService suggestionProduitService,
        LotService lotService,
        InventoryTransactionService inventoryTransactionService,
        IdGeneratorService idGeneratorService
    ) {
        super(
            produitRepository,
            salesLineRepository,
            stockProduitRepository,
            logsService,
            suggestionProduitService,
            lotService,inventoryTransactionService,idGeneratorService
        );
    }

    @Override
    public SalesLine createSaleLineFromDTO(SaleLineDTO dto, Long stockageId) {
        return super.setCommonSaleLine(dto, stockageId);
    }
}
