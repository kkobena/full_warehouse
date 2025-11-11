package com.kobe.warehouse.service.sale;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.SaleLineId;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.errors.DeconditionnementStockOut;
import com.kobe.warehouse.service.errors.StockException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SalesLineService {

    void updateSaleLine(SaleLineDTO dto, SalesLine salesLine);

    SalesLine buildSaleLineFromDTO(SaleLineDTO dto);

    void updateItemQuantityRequested(SaleLineDTO saleLineDTO, SalesLine salesLine, Integer storageId)
        throws StockException, DeconditionnementStockOut;

    void updateItemQuantitySold(SalesLine salesLine, SaleLineDTO saleLineDTO, Integer storageId);

    void updateItemRegularPrice(SaleLineDTO saleLineDTO, SalesLine salesLine, Integer storageId);

    void updateSaleLine(SaleLineDTO dto, SalesLine salesLine, Integer storageId) throws StockException;

    SalesLine create(SaleLineDTO dto, Integer storageId, Sales sales);

    SalesLine getOneById(SaleLineId id);

    SalesLine createSaleLineFromDTO(SaleLineDTO saleLine, Integer stockageId);

    void processUg(SalesLine salesLine, SaleLineDTO dto, Integer stockageId);

    void deleteSaleLine(SalesLine salesLine);

    Optional<SalesLine> findBySalesIdAndProduitId(SaleId salesId, Integer produitId);

    void saveSalesLine(SalesLine salesLine);

    void cloneSalesLine(Set<SalesLine> salesLines, Sales copy, AppUser user, Integer storageId);

    void createInventory(SalesLine salesLine, AppUser user, Integer storageId);

    void createInventory(Set<SalesLine> salesLines, AppUser user, Integer storageId);

    void save(Set<SalesLine> salesLines, AppUser user, Integer storageId);

    void processProductDiscount(SalesLine salesLine);

    List<SaleLineDTO> findBySalesIdAndSalesSaleDateOrderByProduitLibelle(Long salesId, LocalDate saleDate);
}
