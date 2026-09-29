package com.kobe.warehouse.service.sale.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.kobe.warehouse.domain.enumeration.MotifForcageStock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/** Motif de forçage et quantité servie — lots 1 et 2 de PLAN-VENTE-SUR-STOCK-ERRONE. */
class SalesLineServiceImplForcageTest {

    @Test
    void forcingWithShortfallIsAStockoutByDefault() {
        assertEquals(MotifForcageStock.RUPTURE_AVOIR, SalesLineServiceImpl.motifForcage(true, null, 5, 2));
        assertEquals(MotifForcageStock.RUPTURE_AVOIR, SalesLineServiceImpl.motifForcage(true, MotifForcageStock.RUPTURE_AVOIR, 1, -3));
    }

    @Test
    void requestedInventoryGapIsKeptWhenThereIsAShortfall() {
        assertEquals(MotifForcageStock.ECART_INVENTAIRE, SalesLineServiceImpl.motifForcage(true, MotifForcageStock.ECART_INVENTAIRE, 5, 2));
        assertEquals(MotifForcageStock.ECART_INVENTAIRE, SalesLineServiceImpl.motifForcage(true, MotifForcageStock.ECART_INVENTAIRE, 2, -3));
    }

    @ParameterizedTest
    @EnumSource(MotifForcageStock.class)
    void noMotifWithoutShortfallWhateverIsRequested(MotifForcageStock demande) {
        assertNull(SalesLineServiceImpl.motifForcage(true, demande, 2, 2));
        assertNull(SalesLineServiceImpl.motifForcage(true, demande, 1, 10));
    }

    @Test
    void noMotifWithoutForcing() {
        assertNull(SalesLineServiceImpl.motifForcage(false, null, 5, 2));
        assertNull(SalesLineServiceImpl.motifForcage(false, MotifForcageStock.ECART_INVENTAIRE, 5, 2));
    }

    @ParameterizedTest(name = "demandé {0}, stock {1} → servi {2}")
    @CsvSource({ "5, 10, 5", "5, 2, 2", "5, 0, 0", "5, -3, 0" })
    void stockoutAndNoMotifServeAtMostTheStock(int demande, int stock, int servi) {
        assertEquals(servi, SalesLineServiceImpl.calculateQuantitySold(demande, stock, MotifForcageStock.RUPTURE_AVOIR));
        assertEquals(servi, SalesLineServiceImpl.calculateQuantitySold(demande, stock, null));
    }

    @ParameterizedTest(name = "demandé {0}, stock {1} → servi {0}")
    @CsvSource({ "5, 10", "5, 2", "5, 0", "5, -3" })
    void inventoryGapServesTheWholeRequest(int demande, int stock) {
        assertEquals(demande, SalesLineServiceImpl.calculateQuantitySold(demande, stock, MotifForcageStock.ECART_INVENTAIRE));
    }
}
