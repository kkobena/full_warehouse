package com.kobe.warehouse.service.sale;

import com.kobe.warehouse.domain.RemiseProduit;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.errors.DeconditionnementStockOut;
import com.kobe.warehouse.service.errors.StockException;
import java.util.Optional;
import java.util.Set;

public interface SalesLineService {
    Sales createSaleLine(SaleLineDTO saleLine, Sales sale, Long stockageId) throws StockException;

    void updateSaleLine(SaleLineDTO dto, SalesLine salesLine);

    SalesLine buildSaleLineFromDTO(SaleLineDTO dto);

    void updateItemQuantityRequested(SaleLineDTO saleLineDTO, SalesLine salesLine, Long storageId)
        throws StockException, DeconditionnementStockOut;

    void updateItemQuantitySold(SalesLine salesLine, SaleLineDTO saleLineDTO, Long storageId);

    void updateItemRegularPrice(SaleLineDTO saleLineDTO, SalesLine salesLine, Long storageId);

    void updateSaleLine(SaleLineDTO dto, SalesLine salesLine, Long storageId);

    SalesLine create(SaleLineDTO dto, Long storageId, Sales sales);

    SalesLine getOneById(Long id);

    SalesLine createSaleLineFromDTO(SaleLineDTO saleLine, Long stockageId);

    void processUg(SalesLine salesLine, SaleLineDTO dto, Long stockageId);

    void processProductDiscount(RemiseProduit remiseProduit, SalesLine salesLine);

    void deleteSaleLine(SalesLine salesLine);

    Optional<SalesLine> findBySalesIdAndProduitId(Long salesId, Long produitId);

    void saveSalesLine(SalesLine salesLine);

    void cloneSalesLine(Set<SalesLine> salesLines, Sales copy, User user, Long storageId);

    void createInventory(SalesLine salesLine, User user, Long storageId);

    void createInventory(Set<SalesLine> salesLines, User user, Long storageId);

    void save(Set<SalesLine> salesLines, User user, Long storageId);
}
