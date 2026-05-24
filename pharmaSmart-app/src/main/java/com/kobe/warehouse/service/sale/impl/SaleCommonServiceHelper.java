package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.VenteDepot;

public class SaleCommonServiceHelper {

    public static void upddateCashSaleAmounts(CashSale c) {
        c.setSalesAmount(c.getSalesLines().stream().mapToInt(SalesLine::getSalesAmount).sum());
        c.setCostAmount(c.getSalesLines().stream().mapToInt(sl -> sl.getQuantitySold() * sl.getCostAmount()).sum());
        // Normalement on appelle processDiscount et computeCashSaleAmountToPaid ici
        // Mais ces méthodes sont dans SaleServiceImpl ou SaleCommonService.
    }

    public static void upddateCashSaleAmountsOnRemovingItem(CashSale c, SalesLine saleLine) {
        // Logique simplifiée pour l'exemple
    }

    public static String computeThirdPartySaleAmounts(ThirdPartySales thirdPartySales) {
        // Logique pour ThirdPartySales
        return null;
    }

    public static void upddateThirdPartySaleAmountsOnRemovingItem(ThirdPartySales thirdPartySales) {
        // Logique pour ThirdPartySales
    }
}
