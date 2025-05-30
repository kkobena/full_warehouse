package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.domain.enumeration.TypeVente;
import com.kobe.warehouse.service.sale.SalesLineService;
import org.springframework.stereotype.Component;

@Component
public class SaleLineServiceFactory {

    private final CashSaleLineServiceImpl cashSaleLineService;
    private final AssuranceSaleLineServiceImpl assuranceSaleLineService;

    public SaleLineServiceFactory(CashSaleLineServiceImpl cashSaleLineService, AssuranceSaleLineServiceImpl assuranceSaleLineService) {
        this.cashSaleLineService = cashSaleLineService;
        this.assuranceSaleLineService = assuranceSaleLineService;
    }

    public SalesLineService getService(TypeVente type) {
        if (type == TypeVente.CashSale) {
            return cashSaleLineService;
        } else if (type == TypeVente.ThirdPartySales) {
            return assuranceSaleLineService;
        }
        throw new IllegalArgumentException("Unknown service type: " + type);
    }
}
