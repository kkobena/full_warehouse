package com.kobe.warehouse.service.sale.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.enumeration.TypeVente;
import java.util.LinkedHashSet;
import org.junit.jupiter.api.Test;

class SaleCommonServiceHelperTest {

    @Test
    void recalculatesCashSaleAmountsFromLines() {
        CashSale sale = new CashSale();
        sale.setSalesLines(new LinkedHashSet<>());
        sale.getSalesLines().add(line(1L, 1_000, 2, 300));
        sale.getSalesLines().add(line(2L, 450, 3, 100));

        SaleCommonServiceHelper.upddateCashSaleAmounts(sale);

        assertEquals(1_450, sale.getSalesAmount());
        assertEquals(900, sale.getCostAmount());
    }

    @Test
    void placeholderOperationsHaveNoSideEffectAndThirdPartyCalculationReturnsNull() {
        CashSale cashSale = new CashSale();
        cashSale.setSalesAmount(700);
        SalesLine line = line(1L, 200, 1, 50);
        ThirdPartySales thirdPartySales = new ThirdPartySales();
        thirdPartySales.setSalesAmount(900);

        SaleCommonServiceHelper.upddateCashSaleAmountsOnRemovingItem(cashSale, line);
        SaleCommonServiceHelper.upddateThirdPartySaleAmountsOnRemovingItem(thirdPartySales);

        assertEquals(700, cashSale.getSalesAmount());
        assertEquals(900, thirdPartySales.getSalesAmount());
        assertNull(SaleCommonServiceHelper.computeThirdPartySaleAmounts(thirdPartySales));
    }

    @Test
    void factoryAlwaysReturnsItsConfiguredService() {
        SalesLineServiceBaseImpl lineService = mock(SalesLineServiceBaseImpl.class);
        SaleLineServiceFactory factory = new SaleLineServiceFactory(lineService);

        assertSame(lineService, factory.getService(TypeVente.CashSale));
        assertSame(lineService, factory.getService(TypeVente.ThirdPartySales));
        assertSame(lineService, factory.getService(TypeVente.VenteDepot));
        assertSame(lineService, factory.getService(null));
    }

    private SalesLine line(long id, int salesAmount, int quantitySold, int costAmount) {
        SalesLine line = new SalesLine();
        line.setId(id);
        line.setSalesAmount(salesAmount);
        line.setQuantitySold(quantitySold);
        line.setCostAmount(costAmount);
        return line;
    }
}

