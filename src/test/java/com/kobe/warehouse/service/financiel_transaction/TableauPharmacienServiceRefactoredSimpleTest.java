package com.kobe.warehouse.service.financiel_transaction;

import static org.junit.jupiter.api.Assertions.*;

import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienWrapper;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Simple test to verify calculator and aggregator work correctly
 */
class TableauPharmacienServiceRefactoredSimpleTest {

    private TableauPharmacienCalculator calculator;
    private TableauPharmacienAggregator aggregator;

    @BeforeEach
    void setUp() {
        calculator = new TableauPharmacienCalculator();
        aggregator = new TableauPharmacienAggregator(calculator);
    }

    @Test
    void testCalculatorWorks() {
        TableauPharmacienWrapper wrapper = new TableauPharmacienWrapper();
        wrapper.setMontantVenteNet(10000L);
        wrapper.setMontantAchatNet(8000L);
        wrapper.setMontantAvoirFournisseur(1000L);

        calculator.calculateRatioVenteAchat(wrapper);

        // 10000 / (8000 - 1000) = 10000 / 7000 = 1.42
        assertNotEquals(0f, wrapper.getRatioVenteAchat());
        assertTrue(wrapper.getRatioVenteAchat() > 0);
    }

    @Test
    void testAggregatorCalculatesTotalAvoirs() {
        TableauPharmacienDTO dto1 = new TableauPharmacienDTO();
        dto1.setMvtDate(LocalDate.of(2025, 1, 1));
        dto1.setMontantAvoirFournisseur(500L);

        TableauPharmacienDTO dto2 = new TableauPharmacienDTO();
        dto2.setMvtDate(LocalDate.of(2025, 1, 2));
        dto2.setMontantAvoirFournisseur(1000L);

        long total = aggregator.calculateTotalSupplierReturns(java.util.List.of(dto1, dto2));

        assertEquals(1500L, total);
    }

    @Test
    void testAggregatorSalesToWrapper() {
        TableauPharmacienWrapper wrapper = new TableauPharmacienWrapper();

        TableauPharmacienDTO dto = new TableauPharmacienDTO();
        dto.setMontantCredit(500L);
        dto.setMontantComptant(800L);
        dto.setMontantHt(1000L);
        dto.setMontantTtc(1200L);
        dto.setMontantTaxe(100L);
        dto.setMontantRemise(50L);
        dto.setMontantNet(1100L);
        dto.setNombreVente(5);

        aggregator.aggregateSalesToWrapper(wrapper, dto);

        assertEquals(500L, wrapper.getMontantVenteCredit());
        assertEquals(800L, wrapper.getMontantVenteComptant());
        assertEquals(1200L, wrapper.getMontantVenteTtc());
        assertEquals(1100L, wrapper.getMontantVenteNet());
        assertEquals(5, wrapper.getNumberCount());
    }
}
