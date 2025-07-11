package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.repository.InventoryTransactionRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.dto.SaleLineDTO;
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
        InventoryTransactionRepository inventoryTransactionRepository,
        LogsService logsService,
        SuggestionProduitService suggestionProduitService,
        LotService lotService
    ) {
        super(
            produitRepository,
            salesLineRepository,
            stockProduitRepository,
            inventoryTransactionRepository,
            logsService,
            suggestionProduitService,
            lotService
        );
    }

    @Override
    public SalesLine createSaleLineFromDTO(SaleLineDTO dto, Long stockageId) {
        return super.setCommonSaleLine(dto, stockageId);
    }
}
