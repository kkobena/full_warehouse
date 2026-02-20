package com.kobe.warehouse.service.sale;

import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.errors.DeconditionnementStockOut;
import com.kobe.warehouse.service.errors.PlafondVenteException;
import com.kobe.warehouse.service.errors.StockException;

public interface SalesManager {
    /**
     * Updates the quantity requested for a sales line and updates the sale's totals and customer display.
     */
    SaleLineDTO updateItemQuantityRequested(SaleLineDTO saleLineDTO, Sales sales, boolean increment) throws StockException, DeconditionnementStockOut, PlafondVenteException;

    /**
     * Updates the quantity sold for a sales line and updates the sale's totals and customer display.
     */
    SaleLineDTO updateItemQuantitySold(SaleLineDTO saleLineDTO, Sales sales) throws PlafondVenteException;

    /**
     * Updates the regular price for a sales line and updates the sale's totals and customer display.
     */
    SaleLineDTO updateItemRegularPrice(SaleLineDTO saleLineDTO, Sales sales) throws PlafondVenteException;

    /**
     * Adds or updates a sales line in a sale and updates the sale's totals and customer display.
     */
    SaleLineDTO addOrUpdateSaleLine(SaleLineDTO dto, Sales sales) throws StockException, DeconditionnementStockOut, PlafondVenteException;

    /**
     * Deletes a sales line by its ID and updates the sale's totals and customer display.
     */
    void deleteSaleLineById(SalesLine salesLine) throws PlafondVenteException;
    SaleLineDTO incrementItemQuantityRequested(SaleLineDTO saleLineDTO, Sales sales)
        throws StockException, DeconditionnementStockOut, PlafondVenteException ;
}
