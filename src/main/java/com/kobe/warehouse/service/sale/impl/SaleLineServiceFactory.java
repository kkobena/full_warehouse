package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.domain.enumeration.TypeVente;
import com.kobe.warehouse.service.sale.SalesLineService;
import org.springframework.stereotype.Component;

/**
 * Factory for creating SalesLineService instances.
 * Refactored to use a single unified implementation (SalesLineServiceBaseImpl)
 * for both CashSale and ThirdPartySales, eliminating previous code duplication.
 */
@Component
public class SaleLineServiceFactory {

    private final SalesLineServiceBaseImpl salesLineService;

    public SaleLineServiceFactory(SalesLineServiceBaseImpl salesLineService) {
        this.salesLineService = salesLineService;
    }

    public SalesLineService getService(TypeVente type) {
        if (type == TypeVente.CashSale || type == TypeVente.ThirdPartySales) {
            return salesLineService;
        }
        throw new IllegalArgumentException("Unknown service type: " + type);
    }
}
