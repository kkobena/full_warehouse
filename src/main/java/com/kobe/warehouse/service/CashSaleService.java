package com.kobe.warehouse.service;

import com.kobe.warehouse.service.dto.CashSaleDTO;

public interface CashSaleService extends  SaleService{
    CashSaleDTO createCashSale(CashSaleDTO dto);
}
