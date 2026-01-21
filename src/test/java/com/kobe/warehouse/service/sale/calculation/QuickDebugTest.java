package com.kobe.warehouse.service.sale.calculation;

import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.service.sale.calculation.dto.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuickDebugTest {

    @Test
    void debugComplexScenario() {
        TiersPayantCalculationService service = new TiersPayantCalculationService();

        CalculationInput input = new CalculationInput();
        input.setNatureVente(NatureVente.ASSURANCE);
        input.setTotalSalesAmount(new BigDecimal("1755"));

        SaleItemInput saleItem = new SaleItemInput();
        saleItem.setSalesLineId(1L);
        saleItem.setRegularUnitPrice(new BigDecimal("365"));
        saleItem.setQuantity(5);
        saleItem.setTotalSalesAmount(new BigDecimal("1755"));
        saleItem.setDiscountAmount(new BigDecimal("55"));
        saleItem.setPrixAssurances(new ArrayList<>());
        saleItem.setTvaRate(20);

        input.setSaleItems(Collections.singletonList(saleItem));

        TiersPayantInput tp1 = createTiersPayant(1, 0.65f, PrioriteTiersPayant.R0);
        TiersPayantInput tp2 = createTiersPayant(2, 0.15f, PrioriteTiersPayant.R1);
        TiersPayantInput tp3 = createTiersPayant(3, 0.10f, PrioriteTiersPayant.R2);

        input.setTiersPayants(List.of(tp1, tp2, tp3));

        CalculationResult result = service.calculate(input);

        System.out.println("\n=== Test Complex Scenario ===");
        System.out.println("Total Sale: " + result.getTotalSaleAmount());
        System.out.println("Discount: " + result.getDiscountAmount());
        System.out.println("Total TP: " + result.getTotalTiersPayant());
        System.out.println("Patient: " + result.getTotalPatientShare());

        for (TiersPayantLineOutput line : result.getTiersPayantLines()) {
            System.out.printf("  TP%d: %s (taux: %d%%)%n",
                line.getClientTiersPayantId(),
                line.getMontant(),
                line.getFinalTaux());
        }
    }

    @Test
    void debug30_80Coverage() {
        TiersPayantCalculationService service = new TiersPayantCalculationService();

        CalculationInput input = new CalculationInput();
        input.setNatureVente(NatureVente.ASSURANCE);
        input.setTotalSalesAmount(new BigDecimal("3455"));

        SaleItemInput saleItem = new SaleItemInput();
        saleItem.setSalesLineId(1L);
        saleItem.setRegularUnitPrice(new BigDecimal("3455"));
        saleItem.setQuantity(1);
        saleItem.setTotalSalesAmount(new BigDecimal("3455"));
        saleItem.setDiscountAmount(BigDecimal.ZERO);
        saleItem.setPrixAssurances(new ArrayList<>());
        saleItem.setTvaRate(0);

        input.setSaleItems(Collections.singletonList(saleItem));

        TiersPayantInput tp1 = createTiersPayant(1, 0.3f, PrioriteTiersPayant.R0);
        TiersPayantInput tp2 = createTiersPayant(2, 0.8f, PrioriteTiersPayant.R1);

        input.setTiersPayants(List.of(tp1, tp2));

        CalculationResult result = service.calculate(input);

        System.out.println("\n=== Test 30% + 80% ===");
        System.out.println("Total Sale: " + result.getTotalSaleAmount());
        System.out.println("Total TP: " + result.getTotalTiersPayant());
        System.out.println("Patient: " + result.getTotalPatientShare());

        for (TiersPayantLineOutput line : result.getTiersPayantLines()) {
            System.out.printf("  TP%d: %s%n",
                line.getClientTiersPayantId(),
                line.getMontant());
        }

        System.out.println("\nCalculations:");
        System.out.println("TP1 (30%): 3455 × 0.3 = " + new BigDecimal("3455").multiply(new BigDecimal("0.3")));
        System.out.println("TP2 (80%): 3455 × 0.8 = " + new BigDecimal("3455").multiply(new BigDecimal("0.8")));
        System.out.println("Remaining after TP1: 3455 - 1036.5 = " + new BigDecimal("3455").subtract(new BigDecimal("1036.5")));
    }

    @Test
    void debugFourTiersPayants() {
        TiersPayantCalculationService service = new TiersPayantCalculationService();

        CalculationInput input = new CalculationInput();
        input.setNatureVente(NatureVente.ASSURANCE);
        input.setTotalSalesAmount(new BigDecimal("1755"));

        SaleItemInput saleItem = new SaleItemInput();
        saleItem.setSalesLineId(1L);
        saleItem.setRegularUnitPrice(new BigDecimal("1755"));
        saleItem.setQuantity(1);
        saleItem.setTotalSalesAmount(new BigDecimal("1755"));
        saleItem.setDiscountAmount(BigDecimal.ZERO);
        saleItem.setPrixAssurances(new ArrayList<>());
        saleItem.setTvaRate(0);

        input.setSaleItems(Collections.singletonList(saleItem));

        TiersPayantInput tp1 = createTiersPayant(1, 0.65f, PrioriteTiersPayant.R0);
        TiersPayantInput tp2 = createTiersPayant(2, 0.15f, PrioriteTiersPayant.R1);
        TiersPayantInput tp3 = createTiersPayant(3, 0.10f, PrioriteTiersPayant.R2);
        TiersPayantInput tp4 = createTiersPayant(4, 0.10f, PrioriteTiersPayant.R3);

        input.setTiersPayants(List.of(tp1, tp2, tp3, tp4));

        CalculationResult result = service.calculate(input);

        System.out.println("\n=== Test Four TPs ===");
        System.out.println("Total Sale: " + result.getTotalSaleAmount());
        System.out.println("Total TP: " + result.getTotalTiersPayant());
        System.out.println("Patient: " + result.getTotalPatientShare());

        BigDecimal sum = BigDecimal.ZERO;
        for (TiersPayantLineOutput line : result.getTiersPayantLines()) {
            System.out.printf("  TP%d: %s%n",
                line.getClientTiersPayantId(),
                line.getMontant());
            sum = sum.add(line.getMontant());
        }
        System.out.println("Sum of TPs: " + sum);
        System.out.println("Expected: 1755");
    }

    private TiersPayantInput createTiersPayant(int id, float taux, PrioriteTiersPayant priorite) {
        TiersPayantInput tp = new TiersPayantInput();
        tp.setClientTiersPayantId(id);
        tp.setTaux(taux);
        tp.setPriorite(priorite);
        tp.setTiersPayantFullName("TiersPayant_" + id);
        return tp;
    }
}
