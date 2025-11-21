package com.kobe.warehouse.service.sale;

import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.sale.dto.FinalyseSaleDTO;
import org.springframework.data.domain.Slice;

public interface SimplifiedSaleService {

    FinalyseSaleDTO createCashSale(CashSaleDTO dto);

    Slice<CashSaleDTO> getList(String search);
}
