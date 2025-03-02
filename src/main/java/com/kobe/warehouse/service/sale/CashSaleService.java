package com.kobe.warehouse.service.sale;

import com.kobe.warehouse.service.dto.CashSaleDTO;

public interface CashSaleService {
    CashSaleDTO create(CashSaleDTO dto);

    void setCustomer(Long saleId, Long customerId);

    void removeCustomer(Long saleId);
}
