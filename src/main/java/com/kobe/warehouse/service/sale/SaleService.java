package com.kobe.warehouse.service.sale;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Payment;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.KeyValue;
import com.kobe.warehouse.service.dto.PaymentDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.errors.CashRegisterException;
import com.kobe.warehouse.service.errors.DeconditionnementStockOut;
import com.kobe.warehouse.service.errors.PaymentAmountException;
import com.kobe.warehouse.service.errors.SaleNotFoundCustomerException;
import com.kobe.warehouse.service.errors.StockException;
import com.kobe.warehouse.service.sale.dto.FinalyseSaleDTO;

public interface SaleService {
    SaleLineDTO updateSaleLine(SaleLineDTO saleLine);

    CashSaleDTO createCashSale(CashSaleDTO dto);

    SaleLineDTO updateItemQuantityRequested(SaleLineDTO saleLineDTO) throws StockException, DeconditionnementStockOut;

    SaleLineDTO updateItemQuantitySold(SaleLineDTO saleLineDTO);

    SaleLineDTO updateItemRegularPrice(SaleLineDTO saleLineDTO);

    SaleLineDTO addOrUpdateSaleLine(SaleLineDTO dto);

    FinalyseSaleDTO save(CashSaleDTO dto) throws PaymentAmountException, SaleNotFoundCustomerException, CashRegisterException;

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

    void setCustomer(KeyValue keyValue);

    void removeCustomer(Long saleId);
}
