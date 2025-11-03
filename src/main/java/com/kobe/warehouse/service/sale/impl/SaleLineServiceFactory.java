package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.domain.enumeration.TypeVente;
import com.kobe.warehouse.service.sale.SalesLineService;
import org.springframework.stereotype.Component;


@Component
public class SaleLineServiceFactory {


    private final SalesLineServiceBaseImpl salesLineService;


    public SaleLineServiceFactory(SalesLineServiceBaseImpl salesLineService) {
        this.salesLineService = salesLineService;

    }

    public SalesLineService getService(TypeVente type) {
        return salesLineService;
    }
}
