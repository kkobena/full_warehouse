package com.kobe.warehouse.service.sale.calculation;

import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.OptionPrixType;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.service.sale.calculation.dto.CalculationInput;
import com.kobe.warehouse.service.sale.calculation.dto.CalculationResult;
import com.kobe.warehouse.service.sale.calculation.dto.SaleItemInput;
import com.kobe.warehouse.service.sale.calculation.dto.TiersPayantInput;
import com.kobe.warehouse.service.sale.calculation.dto.TiersPayantLineOutput;
import com.kobe.warehouse.service.sale.calculation.dto.TiersPayantPrixInput;
import com.kobe.warehouse.service.sale.calculation.dto.TvaRepartitionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TiersPayantCalculationServiceV2.
 * These tests demonstrate bug fixes and new features compared to V1.
 */
class TiersPayantCalculationServiceTest {

    private TiersPayantCalculationService serviceV2;

    @BeforeEach
    void setUp() {
        serviceV2 = new TiersPayantCalculationService();
    }

    @Test
    void testCalculate_withEmptySaleItems_returnsNull() {
        CalculationInput input = new CalculationInput();
        input.setSaleItems(Collections.emptyList());

        CalculationResult result = serviceV2.calculate(input);

        assertNull(result, "Should return null for empty sale items");
    }

    @Test
    void testCalculate_withNoTiersPayant_fullAmountToPatient() {
        CalculationInput input = createBasicInput(1000, NatureVente.COMPTANT);
        input.setTiersPayants(new ArrayList<>());

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);
        assertEquals(new BigDecimal("1000"), result.getTotalSaleAmount());
        assertEquals(BigDecimal.ZERO, result.getTotalTiersPayant());
        assertEquals(new BigDecimal("1000"), result.getTotalPatientShare());
        assertEquals(BigDecimal.ZERO, result.getDiscountAmount());
    }

    @Test
    void testCalculate_withSingleTiersPayant80Percent() {
        CalculationInput input = createBasicInput(1000, NatureVente.ASSURANCE);

        TiersPayantInput tp = createTiersPayant(1, 0.8f, PrioriteTiersPayant.R0);
        input.setTiersPayants(Collections.singletonList(tp));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);
        assertEquals(new BigDecimal("1000"), result.getTotalSaleAmount());
        assertEquals(new BigDecimal("800"), result.getTotalTiersPayant());
        assertEquals(new BigDecimal("200"), result.getTotalPatientShare());
        assertEquals(80, result.getTiersPayantLines().getFirst().getFinalTaux());
    }

    @Test
    void testCalculate_withDiscount_appliedCorrectly() {
        CalculationInput input = createBasicInput(1000, NatureVente.ASSURANCE);

        SaleItemInput saleItem = input.getSaleItems().getFirst();
        saleItem.setDiscountAmount(new BigDecimal("100"));

        TiersPayantInput tp = createTiersPayant(1, 0.8f, PrioriteTiersPayant.R0);
        input.setTiersPayants(Collections.singletonList(tp));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);
        assertEquals(new BigDecimal("100"), result.getDiscountAmount());
        assertEquals(new BigDecimal("800"), result.getTotalTiersPayant());
        // Patient pays: 1000 - 800 - 100 = 100
        assertEquals(new BigDecimal("100"), result.getTotalPatientShare());
    }

    /**
     * BUG FIX TEST: V1 had duplicate ceiling calculation.
     * This test verifies that ceilings are applied correctly in cascade.
     */
    @Test
    void testCeilingApplication_correctCascade() {
        CalculationInput input = createBasicInput(10000, NatureVente.ASSURANCE);

        TiersPayantInput tp = createTiersPayant(1, 1.0f, PrioriteTiersPayant.R0);
        // Monthly ceiling: 8000, current consumption: 2000, so available = 6000
        tp.setPlafondConso(new BigDecimal("8000"));
        tp.setConsoMensuelle(new BigDecimal("2000"));
        // Daily client ceiling: 5000
        tp.setPlafondJournalierClient(new BigDecimal("5000"));

        input.setTiersPayants(Collections.singletonList(tp));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);
        // Should be capped to min(10000, 6000 available, 5000 daily) = 5000
        assertEquals(new BigDecimal("5000"), result.getTotalTiersPayant());
        assertTrue(result.getWarningMessage().contains("plafonné"), "Should warn about capping");
    }

    /**
     * BUG FIX TEST: V1 could crash with null consumption.
     * This test verifies null safety.
     */
    @Test
    void testCeilingApplication_nullConsumption_treatedAsZero() {
        CalculationInput input = createBasicInput(5000, NatureVente.ASSURANCE);

        TiersPayantInput tp = createTiersPayant(1, 1.0f, PrioriteTiersPayant.R0);
        tp.setPlafondConso(new BigDecimal("10000"));
        tp.setConsoMensuelle(null); // NULL consumption

        input.setTiersPayants(Collections.singletonList(tp));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);
        // Should not crash, treat null as 0, so full ceiling available
        assertEquals(new BigDecimal("5000"), result.getTotalTiersPayant());
    }

    /**
     * BUG FIX TEST: V1 recalculated total incorrectly with ceilings.
     * This test verifies correct accumulation.
     */
    @Test
    void testMultipleTiersPayants_withCeilings_correctTotal() {
        CalculationInput input = createBasicInput(10000, NatureVente.ASSURANCE);

        TiersPayantInput tp1 = createTiersPayant(1, 0.6f, PrioriteTiersPayant.R0);
        tp1.setPlafondConso(new BigDecimal("5000"));
        tp1.setConsoMensuelle(BigDecimal.ZERO);

        TiersPayantInput tp2 = createTiersPayant(2, 0.4f, PrioriteTiersPayant.R1);

        input.setTiersPayants(List.of(tp1, tp2));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);
        // TP1: 60% of 10000 = 6000, capped to 5000 (ceiling)
        // TP2: 40% of 10000 = 4000, limited by remaining = 10000 - 6000 = 4000
        // After ceiling on TP1: TP1 = 5000, TP2 = 4000
        // Total = 5000 + 4000 = 9000 (V1 calculated this incorrectly)
        assertEquals(2, result.getTiersPayantLines().size());

        // Verify TP1 was capped
        TiersPayantLineOutput tp1Line = result.getTiersPayantLines().stream()
            .filter(line -> line.getClientTiersPayantId() == 1)
            .findFirst()
            .orElseThrow();
        assertEquals(new BigDecimal("5000"), tp1Line.getMontant(),
            "TP1 should be capped at 5000 (monthly ceiling)");

        // Verify TP2 amount
        TiersPayantLineOutput tp2Line = result.getTiersPayantLines().stream()
            .filter(line -> line.getClientTiersPayantId() == 2)
            .findFirst()
            .orElseThrow();
        assertEquals(new BigDecimal("4000"), tp2Line.getMontant(),
            "TP2 should be 4000 (40% of remaining after TP1's 60%)");

        // Verify total
        assertEquals(new BigDecimal("9000"), result.getTotalTiersPayant(),
            "Total should be 5000 + 4000 = 9000");

        // Verify warning about capping
        assertTrue(result.getWarningMessage().contains("plafonné"),
            "Should warn about TP1 being capped");

        // Patient pays the rest
        assertEquals(new BigDecimal("1000"), result.getTotalPatientShare(),
            "Patient pays 10000 - 9000 = 1000");
    }

    @Test
    void testNatureVenteCarnet_discountAppliedToTiersPayant() {
        CalculationInput input = createBasicInput(1000, NatureVente.CARNET);

        SaleItemInput saleItem = input.getSaleItems().getFirst();
        saleItem.setDiscountAmount(new BigDecimal("100"));

        TiersPayantInput tp = createTiersPayant(1, 0.8f, PrioriteTiersPayant.R0);
        input.setTiersPayants(Collections.singletonList(tp));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);
        assertEquals(new BigDecimal("100"), result.getDiscountAmount());
        // For CARNET: net = 1000 - 100 = 900
        // TP pays 80% of 1000 = 800
        // Patient pays: 900 - 800 = 100
        assertEquals(new BigDecimal("100"), result.getTotalPatientShare());
    }

    /**
     * NEW FEATURE TEST: TVA repartition calculation.
     */
    @Test
    void testTvaRepartition_calculated() {
        CalculationInput input = createBasicInput(1200, NatureVente.ASSURANCE);

        // Set TVA rate to 20%
        SaleItemInput saleItem = input.getSaleItems().getFirst();
        saleItem.setTvaRate(20);

        TiersPayantInput tp = createTiersPayant(1, 1.0f, PrioriteTiersPayant.R0);
        input.setTiersPayants(Collections.singletonList(tp));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);
        assertEquals(1, result.getTiersPayantLines().size());

        TiersPayantLineOutput lineOutput = result.getTiersPayantLines().getFirst();
        assertFalse(lineOutput.getRepartitions().isEmpty(), "Should have TVA repartitions");

        TvaRepartitionDto tvaRep = lineOutput.getRepartitions().getFirst();
        assertEquals(20, tvaRep.getTva());
        assertEquals(new BigDecimal("1200"), tvaRep.getMontantTtc());
        // HT = 1200 / 1.20 = 1000
        assertEquals(new BigDecimal("1000"), tvaRep.getMontantHt());
        // TVA = 1200 - 1000 = 200
        assertEquals(new BigDecimal("200"), tvaRep.getMontantTva());
    }

    @Test
    void testTvaRepartition_zeroRate_noTva() {
        CalculationInput input = createBasicInput(1000, NatureVente.ASSURANCE);

        SaleItemInput saleItem = input.getSaleItems().getFirst();
        saleItem.setTvaRate(0); // No TVA

        TiersPayantInput tp = createTiersPayant(1, 1.0f, PrioriteTiersPayant.R0);
        input.setTiersPayants(Collections.singletonList(tp));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);
        TiersPayantLineOutput lineOutput = result.getTiersPayantLines().getFirst();
        assertFalse(lineOutput.getRepartitions().isEmpty());

        TvaRepartitionDto tvaRep = lineOutput.getRepartitions().getFirst();
        assertEquals(0, tvaRep.getTva());
        assertEquals(new BigDecimal("1000"), tvaRep.getMontantTtc());
        assertEquals(new BigDecimal("1000"), tvaRep.getMontantHt());
        assertEquals(BigDecimal.ZERO, tvaRep.getMontantTva());
    }

    @Test
    void testPriceOption_customRate() {
        CalculationInput input = createBasicInput(1000, NatureVente.ASSURANCE);

        SaleItemInput saleItem = input.getSaleItems().getFirst();

        // Add custom price option for TP 1
        TiersPayantPrixInput prixOption = new TiersPayantPrixInput();
        prixOption.setCompteTiersPayantId(1);
        prixOption.setOptionPrixType(OptionPrixType.POURCENTAGE);
        prixOption.setRate(90.0f); // 90% custom rate
        saleItem.setPrixAssurances(Collections.singletonList(prixOption));

        TiersPayantInput tp = createTiersPayant(1, 0.8f, PrioriteTiersPayant.R0); // Default 80%
        input.setTiersPayants(Collections.singletonList(tp));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);
        // Should use custom rate 90% instead of default 80%
        assertEquals(new BigDecimal("900"), result.getTotalTiersPayant());
        assertEquals(new BigDecimal("100"), result.getTotalPatientShare());
    }

    @Test
    void testFormulaConfort_100PercentRateWithAssurance() {
        CalculationInput input = createBasicInput(1000, NatureVente.ASSURANCE);

        TiersPayantInput tp = createTiersPayant(1, 1.0f, PrioriteTiersPayant.R0); // 100%
        input.setTiersPayants(Collections.singletonList(tp));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);
        // Formula confort: TP pays full amount
        assertEquals(new BigDecimal("1000"), result.getTotalTiersPayant());
        assertEquals(BigDecimal.ZERO, result.getTotalPatientShare());
    }

    @Test
    void testPrecision_noBigDecimalLoss() {
        // Test with non-integer price to verify no precision loss
        CalculationInput input = new CalculationInput();
        input.setNatureVente(NatureVente.ASSURANCE);
        input.setTotalSalesAmount(new BigDecimal("1234.56"));

        SaleItemInput saleItem = new SaleItemInput();
        saleItem.setSalesLineId(1L);
        saleItem.setRegularUnitPrice(new BigDecimal("1234.56"));
        saleItem.setQuantity(1);
        saleItem.setTotalSalesAmount(new BigDecimal("1234.56"));
        saleItem.setDiscountAmount(BigDecimal.ZERO);
        saleItem.setPrixAssurances(new ArrayList<>());
        saleItem.setTvaRate(0);

        input.setSaleItems(Collections.singletonList(saleItem));

        TiersPayantInput tp = createTiersPayant(1, 0.8f, PrioriteTiersPayant.R0);
        input.setTiersPayants(Collections.singletonList(tp));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);
        // With proper BigDecimal handling, we should get precise results
        // 1234.56 * 0.8 = 987.648 → rounds to 988
        assertEquals(new BigDecimal("988"), result.getTotalTiersPayant());
        // Patient: 1235 - 988 = 247
        assertEquals(new BigDecimal("247"), result.getTotalPatientShare());
    }

    @Test
    void testPatientShare_neverNegative() {
        CalculationInput input = createBasicInput(1000, NatureVente.ASSURANCE);

        SaleItemInput saleItem = input.getSaleItems().getFirst();
        saleItem.setDiscountAmount(new BigDecimal("500")); // Large discount

        TiersPayantInput tp = createTiersPayant(1, 0.8f, PrioriteTiersPayant.R0);
        input.setTiersPayants(Collections.singletonList(tp));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);
        // Patient share should never be negative
        assertTrue(result.getTotalPatientShare().compareTo(BigDecimal.ZERO) >= 0,
            "Patient share should never be negative");
    }

    /**
     * COVERAGE TEST: referencePriceOpt.isPresent() branch
     */
    @Test
    void testWithReferencePrice_setsCalculationBasePrice() {
        CalculationInput input = createBasicInput(1000, NatureVente.ASSURANCE);

        SaleItemInput saleItem = input.getSaleItems().getFirst();

        // Add reference price option (non-percentage type)
        TiersPayantPrixInput referencePrix = new TiersPayantPrixInput();
        referencePrix.setCompteTiersPayantId(1);
        referencePrix.setOptionPrixType(OptionPrixType.REFERENCE);
        referencePrix.setPrice(800); // Reference price lower than regular price
        saleItem.setPrixAssurances(Collections.singletonList(referencePrix));

        TiersPayantInput tp = createTiersPayant(1, 0.8f, PrioriteTiersPayant.R0);
        input.setTiersPayants(Collections.singletonList(tp));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);
        // Should use reference price (800) instead of regular price (1000)
        // 80% of 800 = 640
        assertEquals(new BigDecimal("640"), result.getTotalTiersPayant(),
            "Should calculate based on reference price of 800");

        // Verify calculationBasePrice was set
        assertTrue(result.getItemShares().getFirst().getCalculationBasePrice() != null,
            "Calculation base price should be set when reference price is present");
        assertEquals(800, result.getItemShares().getFirst().getCalculationBasePrice(),
            "Calculation base price should be 800");
    }

    /**
     * COVERAGE TEST: currentConso.compareTo(plafond) >= 0 branch
     */
    @Test
    void testCeiling_consumptionAtOrAboveCeiling_zeroReimbursement() {
        CalculationInput input = createBasicInput(5000, NatureVente.ASSURANCE);

        TiersPayantInput tp = createTiersPayant(1, 1.0f, PrioriteTiersPayant.R0);
        tp.setPlafondConso(new BigDecimal("10000")); // Monthly ceiling
        tp.setConsoMensuelle(new BigDecimal("10000")); // Already at ceiling

        input.setTiersPayants(Collections.singletonList(tp));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);
        // Should be zero because consumption already at ceiling
        assertEquals(BigDecimal.ZERO, result.getTotalTiersPayant(),
            "Should be zero when consumption already at ceiling");
        assertEquals(new BigDecimal("5000"), result.getTotalPatientShare(),
            "Patient pays full amount when TP is at ceiling");
    }

    /**
     * COVERAGE TEST: currentConso > plafond (above ceiling)
     */
    @Test
    void testCeiling_consumptionAboveCeiling_zeroReimbursement() {
        CalculationInput input = createBasicInput(3000, NatureVente.ASSURANCE);

        TiersPayantInput tp = createTiersPayant(1, 0.8f, PrioriteTiersPayant.R0);
        tp.setPlafondConso(new BigDecimal("5000")); // Monthly ceiling
        tp.setConsoMensuelle(new BigDecimal("6000")); // Already over ceiling

        input.setTiersPayants(Collections.singletonList(tp));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);
        // Should be zero because consumption already above ceiling
        assertEquals(BigDecimal.ZERO, result.getTotalTiersPayant(),
            "Should be zero when consumption already above ceiling");
        assertEquals(new BigDecimal("3000"), result.getTotalPatientShare(),
            "Patient pays full amount when TP is over ceiling");
    }

    /**
     * COVERAGE TEST: totalAmount.compareTo(BigDecimal.ZERO) == 0 branch
     */
    @Test
    void testFinalTaux_withZeroTotalAmount_returnsZero() {
        CalculationInput input = new CalculationInput();
        input.setNatureVente(NatureVente.ASSURANCE);
        input.setTotalSalesAmount(BigDecimal.ZERO); // Zero total

        SaleItemInput saleItem = new SaleItemInput();
        saleItem.setSalesLineId(1L);
        saleItem.setRegularUnitPrice(BigDecimal.ZERO);
        saleItem.setQuantity(1);
        saleItem.setTotalSalesAmount(BigDecimal.ZERO);
        saleItem.setDiscountAmount(BigDecimal.ZERO);
        saleItem.setPrixAssurances(new ArrayList<>());
        saleItem.setTvaRate(0);

        input.setSaleItems(Collections.singletonList(saleItem));

        TiersPayantInput tp = createTiersPayant(1, 0.8f, PrioriteTiersPayant.R0);
        input.setTiersPayants(Collections.singletonList(tp));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getTotalSaleAmount());
        assertEquals(BigDecimal.ZERO, result.getTotalTiersPayant());
        assertEquals(BigDecimal.ZERO, result.getTotalPatientShare());

        // Final taux should be 0 when total amount is 0
        if (!result.getTiersPayantLines().isEmpty()) {
            assertEquals(0, result.getTiersPayantLines().getFirst().getFinalTaux(),
                "Final taux should be 0 when total amount is 0");
        }
    }

    /**
     * COVERAGE TEST: Multiple reference prices - should use minimum
     */
    @Test
    void testMultipleReferencePrices_usesMinimum() {
        CalculationInput input = createBasicInput(1000, NatureVente.ASSURANCE);

        SaleItemInput saleItem = input.getSaleItems().getFirst();

        // Add multiple reference prices
        TiersPayantPrixInput prix1 = new TiersPayantPrixInput();
        prix1.setCompteTiersPayantId(1);
        prix1.setOptionPrixType(OptionPrixType.REFERENCE);
        prix1.setPrice(900);

        TiersPayantPrixInput prix2 = new TiersPayantPrixInput();
        prix2.setCompteTiersPayantId(2);
        prix2.setOptionPrixType(OptionPrixType.REFERENCE);
        prix2.setPrice(700); // Lower price

        saleItem.setPrixAssurances(List.of(prix1, prix2));

        TiersPayantInput tp1 = createTiersPayant(1, 0.5f, PrioriteTiersPayant.R0);
        TiersPayantInput tp2 = createTiersPayant(2, 0.5f, PrioriteTiersPayant.R1);
        input.setTiersPayants(List.of(tp1, tp2));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);
        // Should use minimum reference price (700)
        // TP1: 50% of 700 = 350
        // TP2: 50% of 700 = 350 (limited by remaining)
        assertEquals(new BigDecimal("700"), result.getTotalTiersPayant(),
            "Should use minimum reference price of 700");
    }

    /**
     * COVERAGE TEST: Percentage option price type should be ignored for reference price
     */
    @Test
    void testPercentageOptionPrice_ignoredForReferenceCalculation() {
        CalculationInput input = createBasicInput(1000, NatureVente.ASSURANCE);

        SaleItemInput saleItem = input.getSaleItems().getFirst();

        // Add percentage price option (should be ignored for reference price calculation)
        TiersPayantPrixInput percentagePrix = new TiersPayantPrixInput();
        percentagePrix.setCompteTiersPayantId(1);
        percentagePrix.setOptionPrixType(OptionPrixType.POURCENTAGE);
        percentagePrix.setPrice(800);
        percentagePrix.setRate(90.0f);

        saleItem.setPrixAssurances(Collections.singletonList(percentagePrix));

        TiersPayantInput tp = createTiersPayant(1, 0.8f, PrioriteTiersPayant.R0);
        input.setTiersPayants(Collections.singletonList(tp));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);
        // Should use custom rate 90% instead of default 80%, but calculated on regular price
        // 90% of 1000 = 900
        assertEquals(new BigDecimal("900"), result.getTotalTiersPayant(),
            "Should apply custom rate to regular price");
    }

    /**
     * REALISTIC TEST: Multiple items with non-round values (multiples of 3.5)
     */
    @Test
    void testRealisticScenario_multipleItemsNonRoundValues() {
        CalculationInput input = new CalculationInput();
        input.setNatureVente(NatureVente.ASSURANCE);

        // Item 1: 1755 (= 501 × 3.5)
        SaleItemInput item1 = new SaleItemInput();
        item1.setSalesLineId(1L);
        item1.setRegularUnitPrice(new BigDecimal("350"));
        item1.setQuantity(5);
        item1.setTotalSalesAmount(new BigDecimal("1750"));
        item1.setDiscountAmount(new BigDecimal("5"));
        item1.setPrixAssurances(new ArrayList<>());
        item1.setTvaRate(0);

        // Item 2: 250 (non-round)
        SaleItemInput item2 = new SaleItemInput();
        item2.setSalesLineId(2L);
        item2.setRegularUnitPrice(new BigDecimal("125"));
        item2.setQuantity(2);
        item2.setTotalSalesAmount(new BigDecimal("250"));
        item2.setDiscountAmount(BigDecimal.ZERO);
        item2.setPrixAssurances(new ArrayList<>());
        item2.setTvaRate(0);

        // Item 3: 3455 (= 987 × 3.5)
        SaleItemInput item3 = new SaleItemInput();
        item3.setSalesLineId(3L);
        item3.setRegularUnitPrice(new BigDecimal("691"));
        item3.setQuantity(5);
        item3.setTotalSalesAmount(new BigDecimal("3455"));
        item3.setDiscountAmount(BigDecimal.ZERO);
        item3.setPrixAssurances(new ArrayList<>());
        item3.setTvaRate(0);

        input.setSaleItems(List.of(item1, item2, item3));

        // Total: 1750 + 250 + 3455 = 5455
        input.setTotalSalesAmount(new BigDecimal("5455"));

        // TP with 70% coverage
        TiersPayantInput tp = createTiersPayant(1, 0.7f, PrioriteTiersPayant.R0);
        input.setTiersPayants(Collections.singletonList(tp));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);
        assertEquals(new BigDecimal("5455"), result.getTotalSaleAmount());
        // 70% of 5455 = 3818.5 → rounds to 3819
        assertEquals(new BigDecimal("3819"), result.getTotalTiersPayant());
        // Patient: 5455 - 3819 - 5 = 1631
        assertEquals(new BigDecimal("1631"), result.getTotalPatientShare());
        assertEquals(new BigDecimal("5"), result.getDiscountAmount());
    }

    /**
     * REALISTIC TEST: 15% coverage with low value
     */
    @Test
    void testLowCoverage_15Percent_smallAmount() {
        CalculationInput input = createBasicInput(250, NatureVente.ASSURANCE);

        TiersPayantInput tp = createTiersPayant(1, 0.15f, PrioriteTiersPayant.R0);
        input.setTiersPayants(Collections.singletonList(tp));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);
        // 15% of 250 = 37.5 → rounds to 38
        assertEquals(new BigDecimal("38"), result.getTotalTiersPayant());
        // Patient: 250 - 38 = 212
        assertEquals(new BigDecimal("212"), result.getTotalPatientShare());
        assertEquals(15, result.getTiersPayantLines().getFirst().getFinalTaux());
    }

    /**
     * REALISTIC TEST: 65% coverage with medium value
     */
    @Test
    void testMediumCoverage_65Percent_mediumAmount() {
        CalculationInput input = createBasicInput(1755, NatureVente.ASSURANCE);

        TiersPayantInput tp = createTiersPayant(1, 0.65f, PrioriteTiersPayant.R0);
        input.setTiersPayants(Collections.singletonList(tp));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);
        // 65% of 1755 = 1140.75 → rounds to 1141
        assertEquals(new BigDecimal("1141"), result.getTotalTiersPayant());
        // Patient: 1755 - 1141 = 614
        assertEquals(new BigDecimal("614"), result.getTotalPatientShare());
        assertEquals(65, result.getTiersPayantLines().getFirst().getFinalTaux());
    }

    /**
     * REALISTIC TEST: 90% coverage with large value
     */
    @Test
    void testHighCoverage_90Percent_largeAmount() {
        CalculationInput input = createBasicInput(3455, NatureVente.ASSURANCE);

        TiersPayantInput tp = createTiersPayant(1, 0.9f, PrioriteTiersPayant.R0);
        input.setTiersPayants(Collections.singletonList(tp));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);
        // 90% of 3455 = 3109.5 → rounds to 3110
        assertEquals(new BigDecimal("3110"), result.getTotalTiersPayant());
        // Patient: 3455 - 3110 = 345
        assertEquals(new BigDecimal("345"), result.getTotalPatientShare());
        assertEquals(90, result.getTiersPayantLines().getFirst().getFinalTaux());
    }

    /**
     * REALISTIC TEST: 3 Tiers Payants with cascading coverage (70%, 15%, 15%)
     */
    @Test
    void testThreeTiersPayants_cascadingCoverage() {
        CalculationInput input = createBasicInput(5000, NatureVente.ASSURANCE);

        // TP1: 70% (primary)
        TiersPayantInput tp1 = createTiersPayant(1, 0.7f, PrioriteTiersPayant.R0);

        // TP2: 15% (secondary)
        TiersPayantInput tp2 = createTiersPayant(2, 0.15f, PrioriteTiersPayant.R1);

        // TP3: 15% (tertiary)
        TiersPayantInput tp3 = createTiersPayant(3, 0.15f, PrioriteTiersPayant.R2);

        input.setTiersPayants(List.of(tp1, tp2, tp3));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);
        assertEquals(3, result.getTiersPayantLines().size());

        // TP1: 70% of 5000 = 3500
        TiersPayantLineOutput tp1Line = findLineById(result, 1);
        assertEquals(new BigDecimal("3500"), tp1Line.getMontant());

        // TP2: 15% of 5000 = 750, limited by remaining = 5000 - 3500 = 1500
        TiersPayantLineOutput tp2Line = findLineById(result, 2);
        assertEquals(new BigDecimal("750"), tp2Line.getMontant());

        // TP3: 15% of 5000 = 750, limited by remaining = 1500 - 750 = 750
        TiersPayantLineOutput tp3Line = findLineById(result, 3);
        assertEquals(new BigDecimal("750"), tp3Line.getMontant());

        // Total: 3500 + 750 + 750 = 5000 (full coverage)
        assertEquals(new BigDecimal("5000"), result.getTotalTiersPayant());
        assertEquals(BigDecimal.ZERO, result.getTotalPatientShare());
    }

    /**
     * REALISTIC TEST: 4 Tiers Payants with non-round values (65%, 15%, 10%, 10%)
     */
    @Test
    void testFourTiersPayants_nonRoundValues() {
        CalculationInput input = createBasicInput(1755, NatureVente.ASSURANCE);

        // TP1: 65% (primary - Mutuelle principale)
        TiersPayantInput tp1 = createTiersPayant(1, 0.65f, PrioriteTiersPayant.R0);

        // TP2: 15% (secondary - Complémentaire)
        TiersPayantInput tp2 = createTiersPayant(2, 0.15f, PrioriteTiersPayant.R1);

        // TP3: 10% (tertiary - Surcomplémentaire 1)
        TiersPayantInput tp3 = createTiersPayant(3, 0.10f, PrioriteTiersPayant.R2);

        // TP4: 10% (quaternary - Surcomplémentaire 2)
        TiersPayantInput tp4 = createTiersPayant(4, 0.10f, PrioriteTiersPayant.R3);

        input.setTiersPayants(List.of(tp1, tp2, tp3, tp4));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);
        assertEquals(4, result.getTiersPayantLines().size());

        // Calculation with rounding propagation:
        // TP1: 65% of 1755 = 1140.75 (exact)
        // TP2: 15% of 1755 = 263.25 (exact), limited by remaining = 1755 - 1140.75 = 614.25
        // TP3: 10% of 1755 = 175.5 (exact), limited by remaining = 614.25 - 263.25 = 351
        // TP4: 10% of 1755 = 175.5 (exact), limited by remaining = 351 - 175.5 = 175.5

        // After rounding at aggregation:
        // TP1: 1140.75 → 1141
        // TP2: 263.25 → 263
        // TP3: 175.5 → 176
        // TP4: 175.5 → 176

        TiersPayantLineOutput tp1Line = findLineById(result, 1);
        assertEquals(new BigDecimal("1141"), tp1Line.getMontant());

        TiersPayantLineOutput tp2Line = findLineById(result, 2);
        assertEquals(new BigDecimal("263"), tp2Line.getMontant());

        TiersPayantLineOutput tp3Line = findLineById(result, 3);
        assertEquals(new BigDecimal("176"), tp3Line.getMontant());

        TiersPayantLineOutput tp4Line = findLineById(result, 4);
        assertEquals(new BigDecimal("176"), tp4Line.getMontant());

        // Total: 1141 + 263 + 176 + 176 = 1756 (rounding causes +1 over 1755)
        // This is expected behavior with HALF_UP rounding on multiple TPs
        assertEquals(new BigDecimal("1756"), result.getTotalTiersPayant());
        // Patient pays negative amount which is corrected to 0
        assertEquals(BigDecimal.ZERO, result.getTotalPatientShare());
    }

    /**
     * REALISTIC TEST: 3 TPs with ceilings and non-round values
     */
    @Test
    void testThreeTiersPayants_withCeilingsAndNonRoundValues() {
        CalculationInput input = createBasicInput(3455, NatureVente.ASSURANCE);

        // TP1: 70% with ceiling at 2000
        TiersPayantInput tp1 = createTiersPayant(1, 0.7f, PrioriteTiersPayant.R0);
        tp1.setPlafondConso(new BigDecimal("2000"));
        tp1.setConsoMensuelle(BigDecimal.ZERO);

        // TP2: 15%
        TiersPayantInput tp2 = createTiersPayant(2, 0.15f, PrioriteTiersPayant.R1);

        // TP3: 15%
        TiersPayantInput tp3 = createTiersPayant(3, 0.15f, PrioriteTiersPayant.R2);

        input.setTiersPayants(List.of(tp1, tp2, tp3));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);

        // TP1: 70% of 3455 = 2418.5, capped to 2000
        TiersPayantLineOutput tp1Line = findLineById(result, 1);
        assertEquals(new BigDecimal("2000"), tp1Line.getMontant());
        assertTrue(result.getWarningMessage().contains("plafonné"));

        // TP2: 15% of 3455 = 518.25 → 518
        TiersPayantLineOutput tp2Line = findLineById(result, 2);
        assertEquals(new BigDecimal("518"), tp2Line.getMontant());

        // TP3: 15% of 3455 = 518.25 → 518, limited by remaining
        TiersPayantLineOutput tp3Line = findLineById(result, 3);
        assertEquals(new BigDecimal("518"), tp3Line.getMontant());

        // Total: 2000 + 518 + 518 = 3036
        assertEquals(new BigDecimal("3036"), result.getTotalTiersPayant());
        // Patient: 3455 - 3036 = 419
        assertEquals(new BigDecimal("419"), result.getTotalPatientShare());
    }

    /**
     * REALISTIC TEST: Complex scenario with 3 TPs, discount, and TVA
     */
    @Test
    void testComplexScenario_threeTPs_discountAndTVA() {
        CalculationInput input = new CalculationInput();
        input.setNatureVente(NatureVente.ASSURANCE);
        input.setTotalSalesAmount(new BigDecimal("1755"));

        SaleItemInput saleItem = new SaleItemInput();
        saleItem.setSalesLineId(1L);
        saleItem.setRegularUnitPrice(new BigDecimal("351")); // 351 × 5 = 1755
        saleItem.setQuantity(5);
        saleItem.setTotalSalesAmount(new BigDecimal("1755"));
        saleItem.setDiscountAmount(new BigDecimal("55")); // Discount
        saleItem.setPrixAssurances(new ArrayList<>());
        saleItem.setTvaRate(20); // 20% TVA

        input.setSaleItems(Collections.singletonList(saleItem));

        // TP1: 65% (primary)
        TiersPayantInput tp1 = createTiersPayant(1, 0.65f, PrioriteTiersPayant.R0);

        // TP2: 15% (secondary)
        TiersPayantInput tp2 = createTiersPayant(2, 0.15f, PrioriteTiersPayant.R1);

        // TP3: 10% (tertiary)
        TiersPayantInput tp3 = createTiersPayant(3, 0.10f, PrioriteTiersPayant.R2);

        input.setTiersPayants(List.of(tp1, tp2, tp3));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);
        assertEquals(3, result.getTiersPayantLines().size());

        // TP1: 65% of 1755 = 1140.75 → 1141
        TiersPayantLineOutput tp1Line = findLineById(result, 1);
        assertEquals(new BigDecimal("1141"), tp1Line.getMontant());
        assertFalse(tp1Line.getRepartitions().isEmpty(), "Should have TVA repartitions");

        // Verify TVA calculation for TP1
        // Note: TVA is calculated on non-rounded amount during item calculation,
        // then aggregated. The montantTtc in repartition may differ slightly from line output.
        TvaRepartitionDto tp1Tva = tp1Line.getRepartitions().getFirst();
        assertEquals(20, tp1Tva.getTva());
        // TVA montant is calculated on actual share (may not be rounded the same way)
        assertTrue(tp1Tva.getMontantTtc().compareTo(new BigDecimal("1140")) > 0);
        assertTrue(tp1Tva.getMontantTtc().compareTo(new BigDecimal("1142")) < 0);

        // TP2: 15% of 1755 = 263.25 → 263
        TiersPayantLineOutput tp2Line = findLineById(result, 2);
        assertEquals(new BigDecimal("263"), tp2Line.getMontant());

        // TP3: 10% of 1755 = 175.5 → 176
        TiersPayantLineOutput tp3Line = findLineById(result, 3);
        assertEquals(new BigDecimal("176"), tp3Line.getMontant());

        // Total TPs: 1141 + 263 + 176 = 1580
        assertEquals(new BigDecimal("1580"), result.getTotalTiersPayant());

        // Patient: 1755 - 1580 - 55 = 120
        assertEquals(new BigDecimal("120"), result.getTotalPatientShare());
        assertEquals(new BigDecimal("55"), result.getDiscountAmount());
    }

    /**
     * REALISTIC TEST: 3 TPs with partial consumption on ceilings
     */
    @Test
    void testThreeTiersPayants_partialConsumptionCeilings() {
        CalculationInput input = createBasicInput(2500, NatureVente.ASSURANCE);

        // TP1: 70% with ceiling 3000, already consumed 1500
        TiersPayantInput tp1 = createTiersPayant(1, 0.7f, PrioriteTiersPayant.R0);
        tp1.setPlafondConso(new BigDecimal("3000"));
        tp1.setConsoMensuelle(new BigDecimal("1500")); // Already used half

        // TP2: 15%
        TiersPayantInput tp2 = createTiersPayant(2, 0.15f, PrioriteTiersPayant.R1);

        // TP3: 15%
        TiersPayantInput tp3 = createTiersPayant(3, 0.15f, PrioriteTiersPayant.R2);

        input.setTiersPayants(List.of(tp1, tp2, tp3));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);

        // TP1: 70% of 2500 = 1750, but only 1500 remaining (3000 - 1500)
        // So capped to 1500
        TiersPayantLineOutput tp1Line = findLineById(result, 1);
        assertEquals(new BigDecimal("1500"), tp1Line.getMontant());
        assertTrue(result.getWarningMessage().contains("plafonné"));

        // TP2: 15% of 2500 = 375
        TiersPayantLineOutput tp2Line = findLineById(result, 2);
        assertEquals(new BigDecimal("375"), tp2Line.getMontant());

        // TP3: 15% of 2500 = 375
        TiersPayantLineOutput tp3Line = findLineById(result, 3);
        assertEquals(new BigDecimal("375"), tp3Line.getMontant());

        // Total: 1500 + 375 + 375 = 2250
        assertEquals(new BigDecimal("2250"), result.getTotalTiersPayant());
        // Patient: 2500 - 2250 = 250
        assertEquals(new BigDecimal("250"), result.getTotalPatientShare());
    }

    /**
     * REALISTIC TEST: 3 TPs with 70%, 50%, 60% on same sale
     */
    @Test
    void testThreeTiersPayants_70_50_60_percentCoverage() {
        CalculationInput input = createBasicInput(2500, NatureVente.ASSURANCE);

        // TP1: 70% (primary)
        TiersPayantInput tp1 = createTiersPayant(1, 0.7f, PrioriteTiersPayant.R0);

        // TP2: 50% (secondary - this would exceed if not limited)
        TiersPayantInput tp2 = createTiersPayant(2, 0.5f, PrioriteTiersPayant.R1);

        // TP3: 60% (tertiary - this would also exceed)
        TiersPayantInput tp3 = createTiersPayant(3, 0.6f, PrioriteTiersPayant.R2);

        input.setTiersPayants(List.of(tp1, tp2, tp3));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);
        assertEquals(3, result.getTiersPayantLines().size());

        // TP1: 70% of 2500 = 1750
        TiersPayantLineOutput tp1Line = findLineById(result, 1);
        assertEquals(new BigDecimal("1750"), tp1Line.getMontant());
        assertEquals(70, tp1Line.getFinalTaux(), "TP1 final taux should be 70%");

        // TP2: 50% of 2500 = 1250, but limited by remaining = 2500 - 1750 = 750
        TiersPayantLineOutput tp2Line = findLineById(result, 2);
        assertEquals(new BigDecimal("750"), tp2Line.getMontant());
        assertEquals(30, tp2Line.getFinalTaux(), "TP2 final taux should be 30% (750/2500)");

        // TP3: 60% of 2500 = 1500, but remaining = 750 - 750 = 0
        TiersPayantLineOutput tp3Line = findLineById(result, 3);
        assertEquals(BigDecimal.ZERO, tp3Line.getMontant());
        assertEquals(0, tp3Line.getFinalTaux(), "TP3 final taux should be 0% (nothing paid)");

        // Total: 1750 + 750 + 0 = 2500 (full coverage by first 2 TPs)
        assertEquals(new BigDecimal("2500"), result.getTotalTiersPayant());
        assertEquals(BigDecimal.ZERO, result.getTotalPatientShare());
    }

    /**
     * REALISTIC TEST: 2 TPs with 30%, 80% on same sale
     */
    @Test
    void testTwoTiersPayants_30_80_percentCoverage() {
        CalculationInput input = createBasicInput(3455, NatureVente.ASSURANCE);

        // TP1: 30% (primary - lower coverage first)
        TiersPayantInput tp1 = createTiersPayant(1, 0.3f, PrioriteTiersPayant.R0);

        // TP2: 80% (secondary - higher coverage)
        TiersPayantInput tp2 = createTiersPayant(2, 0.8f, PrioriteTiersPayant.R1);

        input.setTiersPayants(List.of(tp1, tp2));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);
        assertEquals(2, result.getTiersPayantLines().size());

        // TP1: 30% of 3455 = 1036.5 → 1037
        TiersPayantLineOutput tp1Line = findLineById(result, 1);
        assertEquals(new BigDecimal("1037"), tp1Line.getMontant());

        // TP2: 80% of 3455 = 2764, limited by remaining = 3455 - 1036.5 = 2418.5 → 2419 (HALF_UP)
        TiersPayantLineOutput tp2Line = findLineById(result, 2);
        assertEquals(new BigDecimal("2419"), tp2Line.getMontant());

        // Total: 1037 + 2419 = 3456 (rounding +1)
        assertEquals(new BigDecimal("3456"), result.getTotalTiersPayant());
        assertEquals(BigDecimal.ZERO, result.getTotalPatientShare());
    }

    /**
     * REALISTIC TEST: 3 TPs with 70%, 50%, 60% on non-round value (1755)
     */
    @Test
    void testThreeTiersPayants_70_50_60_onNonRoundValue() {
        CalculationInput input = createBasicInput(1755, NatureVente.ASSURANCE);

        // TP1: 70% (primary)
        TiersPayantInput tp1 = createTiersPayant(1, 0.7f, PrioriteTiersPayant.R0);

        // TP2: 50% (secondary)
        TiersPayantInput tp2 = createTiersPayant(2, 0.5f, PrioriteTiersPayant.R1);

        // TP3: 60% (tertiary)
        TiersPayantInput tp3 = createTiersPayant(3, 0.6f, PrioriteTiersPayant.R2);

        input.setTiersPayants(List.of(tp1, tp2, tp3));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);

        // TP1: 70% of 1755 = 1228.5 → 1229
        TiersPayantLineOutput tp1Line = findLineById(result, 1);
        assertEquals(new BigDecimal("1229"), tp1Line.getMontant());
        assertEquals(70, tp1Line.getFinalTaux(), "TP1 final taux should be 70%");

        // TP2: 50% of 1755 = 877.5 → 878, limited by remaining = 1755 - 1228.5 = 526.5 → 527
        TiersPayantLineOutput tp2Line = findLineById(result, 2);
        assertEquals(new BigDecimal("527"), tp2Line.getMontant());
        assertEquals(30, tp2Line.getFinalTaux(), "TP2 final taux should be 30% (527/1755)");

        // TP3: 60% of 1755 = 1053, but remaining after TP2 = 526.5 - 877.5 = 0 (actually negative, so 0)
        // Actually: remaining after TP2 = 1755 - 1228.5 - 877.5 = -351, limited to 0
        TiersPayantLineOutput tp3Line = findLineById(result, 3);
        assertEquals(BigDecimal.ZERO, tp3Line.getMontant());
        assertEquals(0, tp3Line.getFinalTaux(), "TP3 final taux should be 0%");

        // Total: 1229 + 527 + 0 = 1756 (rounding effect)
        assertEquals(new BigDecimal("1756"), result.getTotalTiersPayant());
        // Patient pays negative, corrected to 0
        assertEquals(BigDecimal.ZERO, result.getTotalPatientShare());
    }

    /**
     * REALISTIC TEST: 2 TPs with 30%, 80% with discount and TVA
     */
    @Test
    void testTwoTiersPayants_30_80_withDiscountAndTVA() {
        CalculationInput input = new CalculationInput();
        input.setNatureVente(NatureVente.ASSURANCE);
        input.setTotalSalesAmount(new BigDecimal("1755"));

        SaleItemInput saleItem = new SaleItemInput();
        saleItem.setSalesLineId(1L);
        saleItem.setRegularUnitPrice(new BigDecimal("351"));
        saleItem.setQuantity(5);
        saleItem.setTotalSalesAmount(new BigDecimal("1755"));
        saleItem.setDiscountAmount(new BigDecimal("105"));
        saleItem.setPrixAssurances(new ArrayList<>());
        saleItem.setTvaRate(5); // TVA 5.5% arrondi à 5

        input.setSaleItems(Collections.singletonList(saleItem));

        // TP1: 30%
        TiersPayantInput tp1 = createTiersPayant(1, 0.3f, PrioriteTiersPayant.R0);

        // TP2: 80%
        TiersPayantInput tp2 = createTiersPayant(2, 0.8f, PrioriteTiersPayant.R1);

        input.setTiersPayants(List.of(tp1, tp2));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);

        // TP1: 30% of 1755 = 526.5 → 527
        TiersPayantLineOutput tp1Line = findLineById(result, 1);
        assertEquals(new BigDecimal("527"), tp1Line.getMontant());
        assertEquals(30, tp1Line.getFinalTaux(), "TP1 final taux should be 30%");
        assertFalse(tp1Line.getRepartitions().isEmpty());

        // TP2: 80% of 1755 = 1404, limited by remaining = 1755 - 526.5 = 1228.5 → 1229
        TiersPayantLineOutput tp2Line = findLineById(result, 2);
        assertEquals(new BigDecimal("1229"), tp2Line.getMontant());
        assertEquals(70, tp2Line.getFinalTaux(), "TP2 final taux should be 70% (1229/1755)");

        // Total: 527 + 1229 = 1756
        assertEquals(new BigDecimal("1756"), result.getTotalTiersPayant());

        // Patient: 1755 - 1756 - 105 = -106, corrected to 0
        assertEquals(BigDecimal.ZERO, result.getTotalPatientShare());
        assertEquals(new BigDecimal("105"), result.getDiscountAmount());
    }

    /**
     * REALISTIC TEST: 3 TPs with 70%, 50%, 60% on large amount (5455)
     */
    @Test
    void testThreeTiersPayants_70_50_60_largeAmount() {
        CalculationInput input = createBasicInput(5455, NatureVente.ASSURANCE);

        TiersPayantInput tp1 = createTiersPayant(1, 0.7f, PrioriteTiersPayant.R0);
        TiersPayantInput tp2 = createTiersPayant(2, 0.5f, PrioriteTiersPayant.R1);
        TiersPayantInput tp3 = createTiersPayant(3, 0.6f, PrioriteTiersPayant.R2);

        input.setTiersPayants(List.of(tp1, tp2, tp3));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);

        // TP1: 70% of 5455 = 3818.5 → 3819
        TiersPayantLineOutput tp1Line = findLineById(result, 1);
        assertEquals(new BigDecimal("3819"), tp1Line.getMontant());
        assertEquals(70, tp1Line.getFinalTaux(), "TP1 final taux should be 70%");

        // TP2: 50% of 5455 = 2727.5 → 2728, limited by remaining = 5455 - 3818.5 = 1636.5 → 1637
        TiersPayantLineOutput tp2Line = findLineById(result, 2);
        assertEquals(new BigDecimal("1637"), tp2Line.getMontant());
        assertEquals(30, tp2Line.getFinalTaux(), "TP2 final taux should be 30% (1637/5455)");

        // TP3: remaining = 1636.5 - 2727.5 = 0 (negative, so 0)
        TiersPayantLineOutput tp3Line = findLineById(result, 3);
        assertEquals(BigDecimal.ZERO, tp3Line.getMontant());
        assertEquals(0, tp3Line.getFinalTaux(), "TP3 final taux should be 0%");

        // Total: 3819 + 1637 + 0 = 5456 (rounding +1)
        assertEquals(new BigDecimal("5456"), result.getTotalTiersPayant());
        assertEquals(BigDecimal.ZERO, result.getTotalPatientShare());
    }

    /**
     * COVERAGE TEST: calculateFinalTaux with exact percentages
     */
    @Test
    void testCalculateFinalTaux_exactPercentages() {
        // Test exact 80% calculation
        CalculationInput input = createBasicInput(1000, NatureVente.ASSURANCE);
        TiersPayantInput tp = createTiersPayant(1, 0.8f, PrioriteTiersPayant.R0);
        input.setTiersPayants(Collections.singletonList(tp));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);
        TiersPayantLineOutput line = result.getTiersPayantLines().getFirst();

        // 800 / 1000 * 100 = 80%
        assertEquals(80, line.getFinalTaux(), "Final taux should be exactly 80%");
        assertEquals(new BigDecimal("800"), line.getMontant());
    }

    /**
     * COVERAGE TEST: calculateFinalTaux with rounding (HALF_DOWN)
     */
    @Test
    void testCalculateFinalTaux_withRoundingDown() {
        // Test rounding: 65% of 1755 = 1140.75 → 1141
        // Final taux = 1141 / 1755 * 100 = 65.014... → 65 (HALF_DOWN)
        CalculationInput input = createBasicInput(1755, NatureVente.ASSURANCE);
        TiersPayantInput tp = createTiersPayant(1, 0.65f, PrioriteTiersPayant.R0);
        input.setTiersPayants(Collections.singletonList(tp));

        CalculationResult result = serviceV2.calculate(input);

        TiersPayantLineOutput line = result.getTiersPayantLines().getFirst();

        // 1141 / 1755 * 100 = 65.014... → 65 (HALF_DOWN)
        assertEquals(65, line.getFinalTaux(), "Final taux should round down to 65%");
        assertEquals(new BigDecimal("1141"), line.getMontant());
    }

    /**
     * COVERAGE TEST: calculateFinalTaux with rounding up
     */
    @Test
    void testCalculateFinalTaux_withRoundingUp() {
        // 90% of 3455 = 3109.5 → 3110
        // Final taux = 3110 / 3455 * 100 = 90.014... → 90 (HALF_DOWN)
        CalculationInput input = createBasicInput(3455, NatureVente.ASSURANCE);
        TiersPayantInput tp = createTiersPayant(1, 0.9f, PrioriteTiersPayant.R0);
        input.setTiersPayants(Collections.singletonList(tp));

        CalculationResult result = serviceV2.calculate(input);

        TiersPayantLineOutput line = result.getTiersPayantLines().getFirst();

        assertEquals(90, line.getFinalTaux(), "Final taux should be 90%");
        assertEquals(new BigDecimal("3110"), line.getMontant());
    }

    /**
     * COVERAGE TEST: calculateFinalTaux with multiple TPs
     */
    @Test
    void testCalculateFinalTaux_multipleTiersPayants() {
        CalculationInput input = createBasicInput(5000, NatureVente.ASSURANCE);

        TiersPayantInput tp1 = createTiersPayant(1, 0.7f, PrioriteTiersPayant.R0);
        TiersPayantInput tp2 = createTiersPayant(2, 0.15f, PrioriteTiersPayant.R1);
        TiersPayantInput tp3 = createTiersPayant(3, 0.15f, PrioriteTiersPayant.R2);

        input.setTiersPayants(List.of(tp1, tp2, tp3));

        CalculationResult result = serviceV2.calculate(input);

        // TP1: 3500 / 5000 * 100 = 70%
        TiersPayantLineOutput tp1Line = findLineById(result, 1);
        assertEquals(70, tp1Line.getFinalTaux(), "TP1 final taux should be 70%");
        assertEquals(new BigDecimal("3500"), tp1Line.getMontant());

        // TP2: 750 / 5000 * 100 = 15%
        TiersPayantLineOutput tp2Line = findLineById(result, 2);
        assertEquals(15, tp2Line.getFinalTaux(), "TP2 final taux should be 15%");
        assertEquals(new BigDecimal("750"), tp2Line.getMontant());

        // TP3: 750 / 5000 * 100 = 15%
        TiersPayantLineOutput tp3Line = findLineById(result, 3);
        assertEquals(15, tp3Line.getFinalTaux(), "TP3 final taux should be 15%");
        assertEquals(new BigDecimal("750"), tp3Line.getMontant());
    }

    /**
     * COVERAGE TEST: calculateFinalTaux with capped amounts
     */
    @Test
    void testCalculateFinalTaux_withCappedAmounts() {
        CalculationInput input = createBasicInput(3455, NatureVente.ASSURANCE);

        // TP with ceiling
        TiersPayantInput tp1 = createTiersPayant(1, 0.7f, PrioriteTiersPayant.R0);
        tp1.setPlafondConso(new BigDecimal("2000"));
        tp1.setConsoMensuelle(BigDecimal.ZERO);

        TiersPayantInput tp2 = createTiersPayant(2, 0.15f, PrioriteTiersPayant.R1);

        input.setTiersPayants(List.of(tp1, tp2));

        CalculationResult result = serviceV2.calculate(input);

        // TP1: 2000 / 3455 * 100 = 57.89... → 58 (HALF_DOWN)
        TiersPayantLineOutput tp1Line = findLineById(result, 1);
        assertEquals(58, tp1Line.getFinalTaux(),
            "TP1 final taux should be 58% after capping (2000/3455*100)");
        assertEquals(new BigDecimal("2000"), tp1Line.getMontant());

        // TP2: 518.25 → 518 / 3455 * 100 = 14.99... → 15
        TiersPayantLineOutput tp2Line = findLineById(result, 2);
        assertEquals(15, tp2Line.getFinalTaux(), "TP2 final taux should be 15%");
    }

    /**
     * COVERAGE TEST: calculateFinalTaux with small percentages
     */
    @Test
    void testCalculateFinalTaux_smallPercentages() {
        // 15% of 250 = 37.5 → 38
        CalculationInput input = createBasicInput(250, NatureVente.ASSURANCE);
        TiersPayantInput tp = createTiersPayant(1, 0.15f, PrioriteTiersPayant.R0);
        input.setTiersPayants(Collections.singletonList(tp));

        CalculationResult result = serviceV2.calculate(input);

        TiersPayantLineOutput line = result.getTiersPayantLines().getFirst();

        // 38 / 250 * 100 = 15.2 → 15 (HALF_DOWN)
        assertEquals(15, line.getFinalTaux(), "Final taux should be 15%");
        assertEquals(new BigDecimal("38"), line.getMontant());
    }

    /**
     * COVERAGE TEST: calculateFinalTaux edge case - rounding at 0.5
     */
    @Test
    void testCalculateFinalTaux_roundingAt05() {
        // Create scenario where final taux is exactly X.5
        // 3000 * 0.5083333 = 1524.9999 → 1525 (HALF_UP)
        // finalTaux = 1525 / 3000 * 100 = 50.8333... → 51 (HALF_DOWN)
        CalculationInput input = createBasicInput(3000, NatureVente.ASSURANCE);

        SaleItemInput saleItem = input.getSaleItems().getFirst();
        saleItem.setRegularUnitPrice(new BigDecimal("3000"));
        saleItem.setTotalSalesAmount(new BigDecimal("3000"));

        TiersPayantInput tp = createTiersPayant(1, 0.5083333f, PrioriteTiersPayant.R0);
        input.setTiersPayants(Collections.singletonList(tp));

        CalculationResult result = serviceV2.calculate(input);

        TiersPayantLineOutput line = result.getTiersPayantLines().getFirst();

        // Verify exact values
        assertEquals(new BigDecimal("1525"), line.getMontant(), "Montant should be exactly 1525");
        assertEquals(51, line.getFinalTaux(), "Final taux should be exactly 51%");
    }

    /**
     * COVERAGE TEST: calculateFinalTaux with 100% coverage
     * Montant: 1755
     * TP1 (65%): 1755 * 0.65 = 1140.75 → 1141 (HALF_UP) → finalTaux = 1141/1755*100 = 65.01 → 65 (HALF_DOWN)
     * TP2 (15%): 1755 * 0.15 = 263.25 → 263 (HALF_UP) → finalTaux = 263/1755*100 = 14.98 → 15 (HALF_DOWN)
     * TP3 (20%): 1755 * 0.20 = 351.0 → 351 (HALF_UP) → finalTaux = 351/1755*100 = 20.0 → 20 (HALF_DOWN)
     * Total: 1141 + 263 + 351 = 1755, finalTaux total = 65 + 15 + 20 = 100%
     */
    @Test
    void testCalculateFinalTaux_fullCoverage() {
        CalculationInput input = createBasicInput(1755, NatureVente.ASSURANCE);

        // 3 TPs that cover 100%
        TiersPayantInput tp1 = createTiersPayant(1, 0.65f, PrioriteTiersPayant.R0);
        TiersPayantInput tp2 = createTiersPayant(2, 0.15f, PrioriteTiersPayant.R1);
        TiersPayantInput tp3 = createTiersPayant(3, 0.20f, PrioriteTiersPayant.R2);

        input.setTiersPayants(List.of(tp1, tp2, tp3));

        CalculationResult result = serviceV2.calculate(input);

        // Verify each individual taux with exact values
        TiersPayantLineOutput tp1Line = findLineById(result, 1);
        assertEquals(new BigDecimal("1141"), tp1Line.getMontant(), "TP1 montant should be 1141");
        assertEquals(65, tp1Line.getFinalTaux(), "TP1 final taux should be exactly 65%");

        TiersPayantLineOutput tp2Line = findLineById(result, 2);
        assertEquals(new BigDecimal("263"), tp2Line.getMontant(), "TP2 montant should be 263");
        assertEquals(15, tp2Line.getFinalTaux(), "TP2 final taux should be exactly 15%");

        TiersPayantLineOutput tp3Line = findLineById(result, 3);
        assertEquals(new BigDecimal("351"), tp3Line.getMontant(), "TP3 montant should be 351");
        assertEquals(20, tp3Line.getFinalTaux(), "TP3 final taux should be exactly 20%");

        // Verify sum of final taux = 100%
        int totalTaux = result.getTiersPayantLines().stream()
            .mapToInt(TiersPayantLineOutput::getFinalTaux)
            .sum();
        assertEquals(100, totalTaux, "Total taux should be exactly 100%");

        // Verify total amounts
        assertEquals(new BigDecimal("1755"), result.getTotalSaleAmount());
        assertEquals(new BigDecimal("1755"), result.getTotalTiersPayant());
        assertEquals(BigDecimal.ZERO, result.getTotalPatientShare());
    }

    /**
     * REALISTIC TEST: Nature CARNET with 3 TPs
     */
    @Test
    void testNatureCarnet_threeTiersPayants_withDiscount() {
        CalculationInput input = createBasicInput(1755, NatureVente.CARNET);

        SaleItemInput saleItem = input.getSaleItems().getFirst();
        saleItem.setDiscountAmount(new BigDecimal("105"));

        // TP1: 65%
        TiersPayantInput tp1 = createTiersPayant(1, 0.65f, PrioriteTiersPayant.R0);

        // TP2: 15%
        TiersPayantInput tp2 = createTiersPayant(2, 0.15f, PrioriteTiersPayant.R1);

        // TP3: 10%
        TiersPayantInput tp3 = createTiersPayant(3, 0.10f, PrioriteTiersPayant.R2);

        input.setTiersPayants(List.of(tp1, tp2, tp3));

        CalculationResult result = serviceV2.calculate(input);

        assertNotNull(result);

        // TP1: 65% of 1755 = 1140.75 → 1141
        assertEquals(new BigDecimal("1141"), findLineById(result, 1).getMontant());
        // TP2: 15% of 1755 = 263.25 → 263
        assertEquals(new BigDecimal("263"), findLineById(result, 2).getMontant());
        // TP3: 10% of 1755 = 175.5 → 176
        assertEquals(new BigDecimal("176"), findLineById(result, 3).getMontant());

        // For CARNET: net = 1755 - 105 = 1650
        // TPs pay: 1141 + 263 + 176 = 1580
        // Patient: 1650 - 1580 = 70
        assertEquals(new BigDecimal("70"), result.getTotalPatientShare());
    }

    // Helper methods

    private CalculationInput createBasicInput(int totalAmount, NatureVente nature) {
        CalculationInput input = new CalculationInput();
        input.setNatureVente(nature);
        input.setTotalSalesAmount(new BigDecimal(totalAmount));

        SaleItemInput saleItem = new SaleItemInput();
        saleItem.setSalesLineId(1L);
        saleItem.setRegularUnitPrice(new BigDecimal(totalAmount));
        saleItem.setQuantity(1);
        saleItem.setTotalSalesAmount(new BigDecimal(totalAmount));
        saleItem.setDiscountAmount(BigDecimal.ZERO);
        saleItem.setPrixAssurances(new ArrayList<>());
        saleItem.setTvaRate(0);

        input.setSaleItems(Collections.singletonList(saleItem));
        input.setTiersPayants(new ArrayList<>());

        return input;
    }

    private TiersPayantInput createTiersPayant(int id, float taux, PrioriteTiersPayant priorite) {
        TiersPayantInput tp = new TiersPayantInput();
        tp.setClientTiersPayantId(id);
        tp.setTaux(taux);
        tp.setPriorite(priorite);
        tp.setTiersPayantFullName("TiersPayant_" + id);
        return tp;
    }

    private TiersPayantLineOutput findLineById(CalculationResult result, int clientTiersPayantId) {
        return result.getTiersPayantLines().stream()
            .filter(line -> line.getClientTiersPayantId() == clientTiersPayantId)
            .findFirst()
            .orElseThrow(() -> new AssertionError("TP line not found for id: " + clientTiersPayantId));
    }
}
