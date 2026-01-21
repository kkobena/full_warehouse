package com.kobe.warehouse.service.sale.calculation;

import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.service.sale.calculation.dto.CalculationInput;
import com.kobe.warehouse.service.sale.calculation.dto.CalculationResult;
import com.kobe.warehouse.service.sale.calculation.dto.SaleItemInput;
import com.kobe.warehouse.service.sale.calculation.dto.TiersPayantInput;
import com.kobe.warehouse.service.sale.calculation.dto.TiersPayantLineOutput;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Debug test to see actual calculation results.
 */
public class DebugCalculationTest {

    @Test
    void debugMultipleTiersPayantsWithCeilings() {
        TiersPayantCalculationService serviceV2 = new TiersPayantCalculationService();

        // Create input
        CalculationInput input = new CalculationInput();
        input.setNatureVente(NatureVente.ASSURANCE);
        input.setTotalSalesAmount(new BigDecimal("10000"));

        SaleItemInput saleItem = new SaleItemInput();
        saleItem.setSalesLineId(1L);
        saleItem.setRegularUnitPrice(new BigDecimal("10000"));
        saleItem.setQuantity(1);
        saleItem.setTotalSalesAmount(new BigDecimal("10000"));
        saleItem.setDiscountAmount(BigDecimal.ZERO);
        saleItem.setPrixAssurances(new ArrayList<>());
        saleItem.setTvaRate(0);
        input.setSaleItems(Collections.singletonList(saleItem));

        // Create TP1 with ceiling
        TiersPayantInput tp1 = new TiersPayantInput();
        tp1.setClientTiersPayantId(1);
        tp1.setTaux(0.6f);
        tp1.setPriorite(PrioriteTiersPayant.R0);
        tp1.setTiersPayantFullName("TiersPayant_1");
        tp1.setPlafondConso(new BigDecimal("5000"));
        tp1.setConsoMensuelle(BigDecimal.ZERO);

        // Create TP2 without ceiling
        TiersPayantInput tp2 = new TiersPayantInput();
        tp2.setClientTiersPayantId(2);
        tp2.setTaux(0.4f);
        tp2.setPriorite(PrioriteTiersPayant.R1);
        tp2.setTiersPayantFullName("TiersPayant_2");

        input.setTiersPayants(List.of(tp1, tp2));

        // Calculate
        CalculationResult result = serviceV2.calculate(input);

        // Print results
        System.out.println("\n=== DEBUG CALCULATION RESULTS ===");
        System.out.println("Total Sale Amount: " + result.getTotalSaleAmount());
        System.out.println("Total Tiers Payant: " + result.getTotalTiersPayant());
        System.out.println("Total Patient Share: " + result.getTotalPatientShare());
        System.out.println("Discount Amount: " + result.getDiscountAmount());
        System.out.println("\nTiers Payant Lines:");

        for (TiersPayantLineOutput line : result.getTiersPayantLines()) {
            System.out.printf("  TP%d: Montant=%s, Taux=%d%%%n",
                line.getClientTiersPayantId(),
                line.getMontant(),
                line.getFinalTaux()
            );
        }

        System.out.println("\nWarnings: " + result.getWarningMessage());

        System.out.println("\n=== EXPECTED VALUES ===");
        System.out.println("TP1 should be: 5000 (6000 capped to 5000)");
        System.out.println("TP2 should be: 4000 (40% of remaining)");
        System.out.println("Total TP should be: 9000");
        System.out.println("Patient should pay: 1000");
    }
}
