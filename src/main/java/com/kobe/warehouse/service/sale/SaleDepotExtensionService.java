package com.kobe.warehouse.service.sale;

import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.SaleLineId;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.DepotExtensionSaleDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.records.UpdateSaleInfo;
import com.kobe.warehouse.service.errors.CashRegisterException;
import com.kobe.warehouse.service.errors.DeconditionnementStockOut;
import com.kobe.warehouse.service.errors.SaleNotFoundCustomerException;
import com.kobe.warehouse.service.errors.StockException;
import com.kobe.warehouse.service.sale.dto.FinalyseSaleDTO;

import java.time.LocalDate;
import java.util.List;

public interface SaleDepotExtensionService   {
    SaleLineDTO updateSaleLine(SaleLineDTO saleLine);

    DepotExtensionSaleDTO create(DepotExtensionSaleDTO dto);

    SaleLineDTO updateItemQuantityRequested(SaleLineDTO saleLineDTO) throws StockException, DeconditionnementStockOut;

    SaleLineDTO updateItemQuantitySold(SaleLineDTO saleLineDTO);

    SaleLineDTO updateItemRegularPrice(SaleLineDTO saleLineDTO);

    SaleLineDTO addOrUpdateSaleLine(SaleLineDTO dto);

    FinalyseSaleDTO save(DepotExtensionSaleDTO dto) throws  SaleNotFoundCustomerException, CashRegisterException;


    void deleteSaleLineById(SaleLineId id);

    void deleteSalePrevente(SaleId id);

    void cancel(SaleId id);


    void processDiscount(UpdateSaleInfo updateSaleInfo);

    void removeRemiseFromSale(SaleId saleId);

    List<SaleLineDTO> findBySalesIdAndSalesSaleDateOrderByProduitLibelle(Long salesId, LocalDate saleDate);
}
