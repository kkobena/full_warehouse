package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Payment;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.PaymentDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.web.rest.errors.CashRegisterException;
import com.kobe.warehouse.web.rest.errors.DeconditionnementStockOut;
import com.kobe.warehouse.web.rest.errors.PaymentAmountException;
import com.kobe.warehouse.web.rest.errors.SaleNotFoundCustomerException;
import com.kobe.warehouse.web.rest.errors.StockException;


public interface SaleService {


    SaleLineDTO updateSaleLine(SaleLineDTO saleLine);

    CashSaleDTO createCashSale(CashSaleDTO dto);


    SaleLineDTO updateItemQuantityRequested(SaleLineDTO saleLineDTO) throws StockException, DeconditionnementStockOut;

    SaleLineDTO updateItemQuantitySold(SaleLineDTO saleLineDTO);

    SaleLineDTO updateItemRegularPrice(SaleLineDTO saleLineDTO);


    SaleLineDTO addOrUpdateSaleLine(SaleLineDTO dto);


    ResponseDTO save(CashSaleDTO dto) throws PaymentAmountException, SaleNotFoundCustomerException, CashRegisterException;

    /*
    Sauvegarder l etat de la vente
     */
    ResponseDTO putCashSaleOnHold(CashSaleDTO dto);


    void deleteSaleLineById(Long id);

    void deleteSalePrevente(Long id);

    void cancelCashSale(Long id);

    CashSale fromDTOOldCashSale(CashSaleDTO dto);

    Payment buildPaymentFromDTO(PaymentDTO dto, Sales sales);
    /*
    Gestin d'ouverture de caisse
     */


}
