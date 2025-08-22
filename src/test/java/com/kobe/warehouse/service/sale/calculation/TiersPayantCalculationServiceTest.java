package com.kobe.warehouse.service.sale.calculation;

import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.OptionPrixType;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.service.sale.calculation.dto.CalculationInput;
import com.kobe.warehouse.service.sale.calculation.dto.CalculationResult;
import com.kobe.warehouse.service.sale.calculation.dto.SaleItemInput;
import com.kobe.warehouse.service.sale.calculation.dto.TiersPayantInput;
import com.kobe.warehouse.service.sale.calculation.dto.TiersPayantPrixInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TiersPayantCalculationServiceTest {

    private TiersPayantCalculationService tiersPayantCalculationService;

    @BeforeEach
    void setUp() {
        tiersPayantCalculationService = new TiersPayantCalculationService();
    }

    @Test
    void testCalculate_withEmptySaleItems() {
        CalculationInput input = new CalculationInput();
        input.setSaleItems(Collections.emptyList());
        CalculationResult result = tiersPayantCalculationService.calculate(input);
        assertNull(result);
    }

    @Test
    void testCalculate_withNoTiersPayant() {
        CalculationInput input = new CalculationInput();
        input.setNatureVente(NatureVente.COMPTANT);
        input.setTotalSalesAmount(BigDecimal.valueOf(1000));
        SaleItemInput saleItem = new SaleItemInput();
        saleItem.setSalesLineId(1L);
        saleItem.setQuantity(1);
        saleItem.setTotalSalesAmount(BigDecimal.valueOf(1000));
        saleItem.setRegularUnitPrice(BigDecimal.valueOf(1000));
        saleItem.setDiscountAmount(BigDecimal.ZERO);
        saleItem.setPrixAssurances(new ArrayList<>());
        input.setSaleItems(Collections.singletonList(saleItem));
        input.setTiersPayants(new ArrayList<>());

        CalculationResult result = tiersPayantCalculationService.calculate(input);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(1000), result.getTotalSaleAmount());
        assertEquals(BigDecimal.ZERO, result.getTotalTiersPayant());
        assertEquals(BigDecimal.valueOf(1000), result.getTotalPatientShare());
        assertEquals(BigDecimal.ZERO, result.getDiscountAmount());
    }

    @Test
    void testCalculate_withSingleTiersPayant() {
        CalculationInput input = new CalculationInput();
        input.setNatureVente(NatureVente.ASSURANCE);
        input.setTotalSalesAmount(BigDecimal.valueOf(1000));

        SaleItemInput saleItem = new SaleItemInput();
        saleItem.setSalesLineId(1L);
        saleItem.setRegularUnitPrice(BigDecimal.valueOf(1000));
        saleItem.setQuantity(1);
        saleItem.setTotalSalesAmount(BigDecimal.valueOf(1000));
        saleItem.setDiscountAmount(BigDecimal.ZERO);
        saleItem.setPrixAssurances(new ArrayList<>());
        input.setSaleItems(Collections.singletonList(saleItem));

        TiersPayantInput tiersPayant = new TiersPayantInput();
        tiersPayant.setClientTiersPayantId(1L);
        tiersPayant.setTaux(0.8f);
        tiersPayant.setPriorite(PrioriteTiersPayant.R0);
        input.setTiersPayants(Collections.singletonList(tiersPayant));

        CalculationResult result = tiersPayantCalculationService.calculate(input);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(1000), result.getTotalSaleAmount());
        assertEquals(0, result.getTotalTiersPayant().compareTo(BigDecimal.valueOf(800.0)));
        assertEquals(0, result.getTotalPatientShare().compareTo(BigDecimal.valueOf(200.0)));
        assertEquals(BigDecimal.ZERO, result.getDiscountAmount());
    }

    @Test
    void testCalculate_withDiscount() {
        CalculationInput input = new CalculationInput();
        input.setNatureVente(NatureVente.ASSURANCE);
        input.setTotalSalesAmount(BigDecimal.valueOf(1000));

        SaleItemInput saleItem = new SaleItemInput();
        saleItem.setSalesLineId(1L);
        saleItem.setQuantity(1);
        saleItem.setTotalSalesAmount(BigDecimal.valueOf(1000));
        saleItem.setRegularUnitPrice(BigDecimal.valueOf(1000));
        saleItem.setDiscountAmount(BigDecimal.valueOf(100));
        saleItem.setPrixAssurances(new ArrayList<>());
        input.setSaleItems(Collections.singletonList(saleItem));

        TiersPayantInput tiersPayant = new TiersPayantInput();
        tiersPayant.setClientTiersPayantId(1L);
        tiersPayant.setTaux(0.8f);
        tiersPayant.setPriorite(PrioriteTiersPayant.R0);
        input.setTiersPayants(Collections.singletonList(tiersPayant));

        CalculationResult result = tiersPayantCalculationService.calculate(input);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(1000), result.getTotalSaleAmount());
        assertEquals(0, result.getTotalTiersPayant().compareTo(BigDecimal.valueOf(800.0)));
        assertEquals(0, result.getTotalPatientShare().compareTo(BigDecimal.valueOf(100.0)));
        assertEquals(BigDecimal.valueOf(100), result.getDiscountAmount());
    }

    @Test
    void testCalculate_withMultipleTiersPayants() {
        CalculationInput input = new CalculationInput();
        input.setNatureVente(NatureVente.ASSURANCE);
        input.setTotalSalesAmount(BigDecimal.valueOf(1000));

        SaleItemInput saleItem = new SaleItemInput();
        saleItem.setSalesLineId(1L);
        saleItem.setQuantity(1);
        saleItem.setTotalSalesAmount(BigDecimal.valueOf(1000));
        saleItem.setRegularUnitPrice(BigDecimal.valueOf(1000));
        saleItem.setDiscountAmount(BigDecimal.ZERO);
        saleItem.setPrixAssurances(new ArrayList<>());
        input.setSaleItems(Collections.singletonList(saleItem));

        TiersPayantInput tp1 = new TiersPayantInput();
        tp1.setClientTiersPayantId(1L);
        tp1.setTaux(0.8f);
        tp1.setPriorite(PrioriteTiersPayant.R0);

        TiersPayantInput tp2 = new TiersPayantInput();
        tp2.setClientTiersPayantId(2L);
        tp2.setTaux(0.2f);
        tp2.setPriorite(PrioriteTiersPayant.R1);

        input.setTiersPayants(Arrays.asList(tp1, tp2));

        CalculationResult result = tiersPayantCalculationService.calculate(input);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(1000), result.getTotalSaleAmount());
        assertEquals(0, result.getTotalTiersPayant().compareTo(BigDecimal.valueOf(1000.0)));
        assertEquals(0, result.getTotalPatientShare().compareTo(BigDecimal.ZERO));
        assertEquals(BigDecimal.ZERO, result.getDiscountAmount());
    }

    @Test
    void testCalculate_withTiersPayantCeiling() {
        CalculationInput input = new CalculationInput();
        input.setNatureVente(NatureVente.ASSURANCE);
        input.setTotalSalesAmount(BigDecimal.valueOf(2000));

        SaleItemInput saleItem = new SaleItemInput();
        saleItem.setSalesLineId(1L);
        saleItem.setQuantity(1);
        saleItem.setTotalSalesAmount(BigDecimal.valueOf(2000));
        saleItem.setRegularUnitPrice(BigDecimal.valueOf(2000));
        saleItem.setDiscountAmount(BigDecimal.ZERO);
        saleItem.setPrixAssurances(new ArrayList<>());
        input.setSaleItems(Collections.singletonList(saleItem));

        TiersPayantInput tiersPayant = new TiersPayantInput();
        tiersPayant.setClientTiersPayantId(1L);
        tiersPayant.setTaux(0.8f);
        tiersPayant.setPriorite(PrioriteTiersPayant.R0);
        tiersPayant.setPlafondConso(BigDecimal.valueOf(1000));
        tiersPayant.setConsoMensuelle(BigDecimal.valueOf(500));
        input.setTiersPayants(Collections.singletonList(tiersPayant));

        CalculationResult result = tiersPayantCalculationService.calculate(input);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(2000), result.getTotalSaleAmount());
        assertEquals(0, result.getTotalTiersPayant().compareTo(BigDecimal.valueOf(500.0)));
        assertEquals(0, result.getTotalPatientShare().compareTo(BigDecimal.valueOf(1500.0)));
        assertEquals(BigDecimal.ZERO, result.getDiscountAmount());
    }

    @Test
    void testCalculate_withTiersPayantSpecificPrice() {
        CalculationInput input = new CalculationInput();
        input.setNatureVente(NatureVente.ASSURANCE);
        input.setTotalSalesAmount(BigDecimal.valueOf(2000));

        SaleItemInput saleItem = new SaleItemInput();
        saleItem.setSalesLineId(1L);
        saleItem.setRegularUnitPrice(BigDecimal.valueOf(2000));
        saleItem.setDiscountAmount(BigDecimal.ZERO);
        saleItem.setQuantity(1);
        saleItem.setTotalSalesAmount(BigDecimal.valueOf(2000));
        TiersPayantPrixInput prixInput = new TiersPayantPrixInput();
        prixInput.setCompteTiersPayantId(1L);
        prixInput.setPrice(1500);
        prixInput.setRate(0.8f);
        prixInput.setOptionPrixType(OptionPrixType.MIXED_REFERENCE_POURCENTAGE);
        saleItem.setPrixAssurances(Collections.singletonList(prixInput));
        input.setSaleItems(Collections.singletonList(saleItem));

        TiersPayantInput tiersPayant = new TiersPayantInput();
        tiersPayant.setClientTiersPayantId(1L);
        tiersPayant.setTaux(0.8f);
        tiersPayant.setPriorite(PrioriteTiersPayant.R0);
        input.setTiersPayants(Collections.singletonList(tiersPayant));

        CalculationResult result = tiersPayantCalculationService.calculate(input);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(2000), result.getTotalSaleAmount());
        assertEquals(0, result.getTotalTiersPayant().compareTo(BigDecimal.valueOf(1200.0)));
        assertEquals(0, result.getTotalPatientShare().compareTo(BigDecimal.valueOf(800.0)));
        assertEquals(0,result.getDiscountAmount().compareTo(BigDecimal.valueOf(0.0)) );
        assertEquals(1,result.getItemShares().getFirst().getRates().size() );
    }


    @Test
    void testCalculate_withSingleTiersPayantRate_zero() {
        CalculationInput input = new CalculationInput();
        input.setNatureVente(NatureVente.ASSURANCE);
        input.setTotalSalesAmount(BigDecimal.valueOf(1000));

        SaleItemInput saleItem = new SaleItemInput();
        saleItem.setSalesLineId(1L);
        saleItem.setRegularUnitPrice(BigDecimal.valueOf(1000));
        saleItem.setQuantity(1);
        saleItem.setTotalSalesAmount(BigDecimal.valueOf(1000));
        saleItem.setDiscountAmount(BigDecimal.ZERO);
        saleItem.setPrixAssurances(new ArrayList<>());
        input.setSaleItems(Collections.singletonList(saleItem));

        TiersPayantInput tiersPayant = new TiersPayantInput();
        tiersPayant.setClientTiersPayantId(1L);
        tiersPayant.setTaux(0.0f);
        tiersPayant.setPriorite(PrioriteTiersPayant.R0);
        input.setTiersPayants(Collections.singletonList(tiersPayant));

        CalculationResult result = tiersPayantCalculationService.calculate(input);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(1000), result.getTotalSaleAmount());
        assertEquals(0, result.getTotalTiersPayant().compareTo(BigDecimal.valueOf(0.0)));
        assertEquals(0, result.getTotalPatientShare().compareTo(BigDecimal.valueOf(1000.0)));
        assertEquals(BigDecimal.ZERO, result.getDiscountAmount());
    }


    @Test
    void testCalculate_withSingleTiersPayantRate_Carnet() {
        CalculationInput input = new CalculationInput();
        input.setNatureVente(NatureVente.CARNET);
        input.setTotalSalesAmount(BigDecimal.valueOf(1750));

        SaleItemInput saleItem = new SaleItemInput();
        saleItem.setSalesLineId(1L);
        saleItem.setRegularUnitPrice(BigDecimal.valueOf(1000));
        saleItem.setQuantity(1);
        saleItem.setTotalSalesAmount(BigDecimal.valueOf(1000));
        saleItem.setDiscountAmount(BigDecimal.ZERO);
        saleItem.setPrixAssurances(new ArrayList<>());
        input.setSaleItems(Collections.singletonList(saleItem));

        SaleItemInput saleItem2 = new SaleItemInput();
        saleItem2.setSalesLineId(21L);
        saleItem2.setQuantity(1);
        saleItem2.setTotalSalesAmount(BigDecimal.valueOf(750));
        saleItem2.setRegularUnitPrice(BigDecimal.valueOf(750));
        saleItem2.setDiscountAmount(BigDecimal.ZERO);
        saleItem2.setPrixAssurances(new ArrayList<>());
        input.setSaleItems(List.of(saleItem,saleItem2));

        TiersPayantInput tiersPayant = new TiersPayantInput();
        tiersPayant.setClientTiersPayantId(1L);
        tiersPayant.setTaux(100.0f);
        tiersPayant.setPriorite(PrioriteTiersPayant.R0);
        input.setTiersPayants(Collections.singletonList(tiersPayant));

        CalculationResult result = tiersPayantCalculationService.calculate(input);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(1750), result.getTotalSaleAmount());
        assertEquals(0, result.getTotalTiersPayant().compareTo(BigDecimal.valueOf(1750.0)));
        assertEquals(0, result.getTotalPatientShare().compareTo(BigDecimal.valueOf(0.0)));
        assertEquals(BigDecimal.ZERO, result.getDiscountAmount());
    }


    @Test
    void testCalculate_withSingleTiersPayantRate_Carnet_with_discount() {
        CalculationInput input = new CalculationInput();
        input.setNatureVente(NatureVente.CARNET);
        input.setTotalSalesAmount(BigDecimal.valueOf(1750));

        SaleItemInput saleItem = new SaleItemInput();
        saleItem.setSalesLineId(1L);
        saleItem.setQuantity(1);
        saleItem.setTotalSalesAmount(BigDecimal.valueOf(1000));
        saleItem.setRegularUnitPrice(BigDecimal.valueOf(1000));
        saleItem.setDiscountAmount(BigDecimal.valueOf(250));
        saleItem.setPrixAssurances(new ArrayList<>());
        input.setSaleItems(Collections.singletonList(saleItem));

        SaleItemInput saleItem2 = new SaleItemInput();
        saleItem2.setQuantity(1);
        saleItem2.setTotalSalesAmount(BigDecimal.valueOf(750));
        saleItem2.setSalesLineId(21L);
        saleItem2.setRegularUnitPrice(BigDecimal.valueOf(750));
        saleItem2.setDiscountAmount(BigDecimal.ZERO);
        saleItem2.setPrixAssurances(new ArrayList<>());
        input.setSaleItems(List.of(saleItem,saleItem2));

        TiersPayantInput tiersPayant = new TiersPayantInput();
        tiersPayant.setClientTiersPayantId(1L);
        tiersPayant.setTaux(100.0f);
        tiersPayant.setPriorite(PrioriteTiersPayant.R0);
        input.setTiersPayants(Collections.singletonList(tiersPayant));

        CalculationResult result = tiersPayantCalculationService.calculate(input);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(1750), result.getTotalSaleAmount());
        assertEquals(0, result.getTotalTiersPayant().compareTo(BigDecimal.valueOf(1500.0)));
        assertEquals(0, result.getTotalPatientShare().compareTo(BigDecimal.valueOf(0.0)));
        assertEquals(0,  result.getDiscountAmount().compareTo(BigDecimal.valueOf(250.0)));
    }

    @Test
    void testCalculate_withTiersPayantSpecificPriceAndTpRate() {
        CalculationInput input = new CalculationInput();
        input.setNatureVente(NatureVente.ASSURANCE);
        input.setTotalSalesAmount(BigDecimal.valueOf(2000));

        SaleItemInput saleItem = new SaleItemInput();
        saleItem.setSalesLineId(1L);
        saleItem.setQuantity(1);
        saleItem.setTotalSalesAmount(BigDecimal.valueOf(2000));
        saleItem.setRegularUnitPrice(BigDecimal.valueOf(2000));
        saleItem.setDiscountAmount(BigDecimal.ZERO);
        TiersPayantPrixInput prixInput = new TiersPayantPrixInput();
        prixInput.setCompteTiersPayantId(1L);
        prixInput.setPrice(1500);
        prixInput.setRate(0.8f);
        prixInput.setOptionPrixType(OptionPrixType.MIXED_REFERENCE_POURCENTAGE);
        saleItem.setPrixAssurances(Collections.singletonList(prixInput));
        input.setSaleItems(Collections.singletonList(saleItem));
        TiersPayantInput tiersPayant = new TiersPayantInput();
        tiersPayant.setClientTiersPayantId(1L);
        tiersPayant.setTaux(0.7f);
        tiersPayant.setPriorite(PrioriteTiersPayant.R0);
        input.setTiersPayants(Collections.singletonList(tiersPayant));
        CalculationResult result = tiersPayantCalculationService.calculate(input);
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(2000), result.getTotalSaleAmount());
        assertEquals(0, result.getTotalTiersPayant().compareTo(BigDecimal.valueOf(1200.0)));
        assertEquals(0, result.getTotalPatientShare().compareTo(BigDecimal.valueOf(800.0)));
        assertEquals(0,result.getDiscountAmount().compareTo(BigDecimal.valueOf(0.0)) );
    }


    @Test
    void testCalculate_withTiersPayantSpecificPriceAndOtherSaleItemsAndTpRate() {
        CalculationInput input = new CalculationInput();
        input.setNatureVente(NatureVente.ASSURANCE);
        input.setTotalSalesAmount(BigDecimal.valueOf(5750));

        SaleItemInput saleItem = new SaleItemInput();
        saleItem.setSalesLineId(1L);
        saleItem.setQuantity(1);
        saleItem.setTotalSalesAmount(BigDecimal.valueOf(2000));
        saleItem.setRegularUnitPrice(BigDecimal.valueOf(2000));
        saleItem.setDiscountAmount(BigDecimal.ZERO);
        TiersPayantPrixInput prixInput = new TiersPayantPrixInput();
        prixInput.setCompteTiersPayantId(1L);
        prixInput.setPrice(1500);
        prixInput.setRate(0.8f);
        prixInput.setOptionPrixType(OptionPrixType.MIXED_REFERENCE_POURCENTAGE);
        saleItem.setPrixAssurances(Collections.singletonList(prixInput));


        SaleItemInput saleItem2 = new SaleItemInput();
        saleItem2.setSalesLineId(2L);
        saleItem2.setQuantity(1);
        saleItem2.setTotalSalesAmount(BigDecimal.valueOf(3750));
        saleItem2.setRegularUnitPrice(BigDecimal.valueOf(3750));
        saleItem2.setDiscountAmount(BigDecimal.ZERO);

        input.setSaleItems(List.of(saleItem,saleItem2));
        TiersPayantInput tiersPayant = new TiersPayantInput();
        tiersPayant.setClientTiersPayantId(1L);
        tiersPayant.setTaux(0.7f);
        tiersPayant.setPriorite(PrioriteTiersPayant.R0);
        input.setTiersPayants(Collections.singletonList(tiersPayant));
        CalculationResult result = tiersPayantCalculationService.calculate(input);
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(5750), result.getTotalSaleAmount());
        assertEquals(0, result.getTotalTiersPayant().compareTo(BigDecimal.valueOf(3825.0)));
        assertEquals(0, result.getTotalPatientShare().compareTo(BigDecimal.valueOf(1925.0)));
        assertEquals(0,result.getDiscountAmount().compareTo(BigDecimal.valueOf(0.0)) );
    }

    @Test
    void testCalculate_withMultipleTiersPayantSpecificPriceAndTpRate() {
        CalculationInput input = new CalculationInput();
        input.setNatureVente(NatureVente.ASSURANCE);
        input.setTotalSalesAmount(BigDecimal.valueOf(5750));

        SaleItemInput saleItem = new SaleItemInput();
        saleItem.setSalesLineId(1L);
        saleItem.setQuantity(1);
        saleItem.setTotalSalesAmount(BigDecimal.valueOf(5750));
        saleItem.setRegularUnitPrice(BigDecimal.valueOf(5750));
        saleItem.setDiscountAmount(BigDecimal.ZERO);
        TiersPayantPrixInput prixInput = new TiersPayantPrixInput();
        prixInput.setCompteTiersPayantId(1L);
        prixInput.setPrice(4750);
        prixInput.setRate(0.8f);
        prixInput.setOptionPrixType(OptionPrixType.MIXED_REFERENCE_POURCENTAGE);

        TiersPayantPrixInput prixInput2 = new TiersPayantPrixInput();
        prixInput2.setCompteTiersPayantId(1L);
        prixInput2.setPrice(4250);
        prixInput2.setRate(0.2f);
        prixInput2.setOptionPrixType(OptionPrixType.MIXED_REFERENCE_POURCENTAGE);
        saleItem.setPrixAssurances(List.of(prixInput,prixInput2));
        input.setSaleItems(Collections.singletonList(saleItem));

        TiersPayantInput tiersPayant = new TiersPayantInput();
        tiersPayant.setClientTiersPayantId(1L);
        tiersPayant.setTaux(0.7f);
        tiersPayant.setPriorite(PrioriteTiersPayant.R0);

        TiersPayantInput tiersPayant1 = new TiersPayantInput();
        tiersPayant1.setClientTiersPayantId(2L);
        tiersPayant1.setTaux(0.20f);
        tiersPayant1.setPriorite(PrioriteTiersPayant.R1);
        input.setTiersPayants(new ArrayList<>(List.of(tiersPayant,tiersPayant1)));
        CalculationResult result = tiersPayantCalculationService.calculate(input);
        assertNotNull(result);
        assertEquals(4250, result.getItemShares().getFirst().getCalculationBasePrice());
        assertEquals(0, result.getTiersPayantLines().getFirst().getMontant().compareTo(BigDecimal.valueOf(3400.0)));
        assertEquals(0, result.getTiersPayantLines().getLast().getMontant().compareTo(BigDecimal.valueOf(850.0)));
        assertEquals(BigDecimal.valueOf(5750), result.getTotalSaleAmount());
        assertEquals(0, result.getTotalTiersPayant().compareTo(BigDecimal.valueOf(4250.0)));
        assertEquals(0, result.getTotalPatientShare().compareTo(BigDecimal.valueOf(1500.0)));
        assertEquals(0,result.getDiscountAmount().compareTo(BigDecimal.valueOf(0.0)) );
    }


    @Test
    void testCalculate_withTiersPayantDailyCeiling() {
        CalculationInput input = new CalculationInput();
        input.setNatureVente(NatureVente.ASSURANCE);
        input.setTotalSalesAmount(BigDecimal.valueOf(2000));

        SaleItemInput saleItem = new SaleItemInput();
        saleItem.setSalesLineId(1L);
        saleItem.setQuantity(1);
        saleItem.setTotalSalesAmount(BigDecimal.valueOf(2000));
        saleItem.setRegularUnitPrice(BigDecimal.valueOf(2000));
        saleItem.setDiscountAmount(BigDecimal.ZERO);
        saleItem.setPrixAssurances(new ArrayList<>());
        input.setSaleItems(Collections.singletonList(saleItem));

        TiersPayantInput tiersPayant = new TiersPayantInput();
        tiersPayant.setClientTiersPayantId(1L);
        tiersPayant.setTaux(0.8f);
        tiersPayant.setPriorite(PrioriteTiersPayant.R0);
        tiersPayant.setPlafondConso(BigDecimal.valueOf(1000));
        tiersPayant.setPlafondJournalierClient(BigDecimal.valueOf(500));
        tiersPayant.setConsoMensuelle(BigDecimal.valueOf(500));
        input.setTiersPayants(Collections.singletonList(tiersPayant));

        CalculationResult result = tiersPayantCalculationService.calculate(input);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(2000), result.getTotalSaleAmount());
        assertEquals(0, result.getTotalTiersPayant().compareTo(BigDecimal.valueOf(500.0)));
        assertEquals(0, result.getTotalPatientShare().compareTo(BigDecimal.valueOf(1500.0)));
        assertEquals(BigDecimal.ZERO, result.getDiscountAmount());
    }


    @Test
    void testCalculate_withTiersPayantDailyCeiling2() {
        CalculationInput input = new CalculationInput();
        input.setNatureVente(NatureVente.ASSURANCE);
        input.setTotalSalesAmount(BigDecimal.valueOf(2000));

        SaleItemInput saleItem = new SaleItemInput();
        saleItem.setSalesLineId(1L);
        saleItem.setQuantity(1);
        saleItem.setTotalSalesAmount(BigDecimal.valueOf(2000));
        saleItem.setRegularUnitPrice(BigDecimal.valueOf(2000));
        saleItem.setDiscountAmount(BigDecimal.ZERO);
        saleItem.setPrixAssurances(new ArrayList<>());
        input.setSaleItems(Collections.singletonList(saleItem));

        TiersPayantInput tiersPayant = new TiersPayantInput();
        tiersPayant.setClientTiersPayantId(1L);
        tiersPayant.setTaux(0.8f);
        tiersPayant.setPriorite(PrioriteTiersPayant.R0);
        tiersPayant.setPlafondJournalierClient(BigDecimal.valueOf(1000));
        input.setTiersPayants(Collections.singletonList(tiersPayant));

        CalculationResult result = tiersPayantCalculationService.calculate(input);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(2000), result.getTotalSaleAmount());
        assertEquals(0, result.getTotalTiersPayant().compareTo(BigDecimal.valueOf(1000.0)));
        assertEquals(0, result.getTotalPatientShare().compareTo(BigDecimal.valueOf(1000.0)));
        assertEquals(BigDecimal.ZERO, result.getDiscountAmount());
    }

    @Test
    void testCalculate_withMultipleTiersPayantSpecificPriceConfort() {
        CalculationInput input = new CalculationInput();
        input.setNatureVente(NatureVente.ASSURANCE);
        input.setTotalSalesAmount(BigDecimal.valueOf(5750));

        SaleItemInput saleItem = new SaleItemInput();
        saleItem.setSalesLineId(1L);
        saleItem.setRegularUnitPrice(BigDecimal.valueOf(5750));
        saleItem.setTotalSalesAmount(BigDecimal.valueOf(5750));
        saleItem.setDiscountAmount(BigDecimal.ZERO);
        saleItem.setQuantity(1);
        saleItem.setTotalSalesAmount(BigDecimal.valueOf(5750));
        TiersPayantPrixInput prixInput = new TiersPayantPrixInput();
        prixInput.setCompteTiersPayantId(1L);
        prixInput.setPrice(4750);
        prixInput.setRate(0.8f);
        prixInput.setOptionPrixType(OptionPrixType.MIXED_REFERENCE_POURCENTAGE);

        TiersPayantPrixInput prixInput2 = new TiersPayantPrixInput();
        prixInput2.setCompteTiersPayantId(1L);
        prixInput2.setPrice(4250);
        prixInput2.setRate(1.f);
        prixInput2.setOptionPrixType(OptionPrixType.MIXED_REFERENCE_POURCENTAGE);
        saleItem.setPrixAssurances(List.of(prixInput,prixInput2));
        input.setSaleItems(Collections.singletonList(saleItem));

        TiersPayantInput tiersPayant = new TiersPayantInput();
        tiersPayant.setClientTiersPayantId(1L);
        tiersPayant.setTaux(0.7f);
        tiersPayant.setPriorite(PrioriteTiersPayant.R0);

        TiersPayantInput tiersPayant1 = new TiersPayantInput();
        tiersPayant1.setClientTiersPayantId(2L);
        tiersPayant1.setTaux(1.0f);
        tiersPayant1.setPriorite(PrioriteTiersPayant.R1);
        input.setTiersPayants(new ArrayList<>(List.of(tiersPayant,tiersPayant1)));
        CalculationResult result = tiersPayantCalculationService.calculate(input);
        assertNotNull(result);
        assertEquals(4250, result.getItemShares().getFirst().getCalculationBasePrice());
        assertEquals(0, result.getTiersPayantLines().getFirst().getMontant().compareTo(BigDecimal.valueOf(3400.0)));
        assertEquals(0, result.getTiersPayantLines().getLast().getMontant().compareTo(BigDecimal.valueOf(2350.0)));
        assertEquals(BigDecimal.valueOf(5750), result.getTotalSaleAmount());
        assertEquals(0, result.getTotalTiersPayant().compareTo(BigDecimal.valueOf(5750.0)));
        assertEquals(0, result.getTotalPatientShare().compareTo(BigDecimal.valueOf(0.0)));
        assertEquals(0,result.getDiscountAmount().compareTo(BigDecimal.valueOf(0.0)) );
    }

    @Test
    void testCalculate_withTiersPayantConsoCeiling() {
        CalculationInput input = new CalculationInput();
        input.setNatureVente(NatureVente.ASSURANCE);
        input.setTotalSalesAmount(BigDecimal.valueOf(2000));

        SaleItemInput saleItem = new SaleItemInput();
        saleItem.setSalesLineId(1L);
        saleItem.setQuantity(1);
        saleItem.setTotalSalesAmount(BigDecimal.valueOf(2000));
        saleItem.setRegularUnitPrice(BigDecimal.valueOf(2000));
        saleItem.setDiscountAmount(BigDecimal.ZERO);
        saleItem.setPrixAssurances(new ArrayList<>());
        input.setSaleItems(Collections.singletonList(saleItem));

        TiersPayantInput tiersPayant = new TiersPayantInput();
        tiersPayant.setClientTiersPayantId(1L);
        tiersPayant.setTaux(0.8f);
        tiersPayant.setPriorite(PrioriteTiersPayant.R0);
        tiersPayant.setPlafondConso(BigDecimal.valueOf(1000));
        tiersPayant.setConsoMensuelle(BigDecimal.valueOf(1000));
        input.setTiersPayants(Collections.singletonList(tiersPayant));

        CalculationResult result = tiersPayantCalculationService.calculate(input);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(2000), result.getTotalSaleAmount());
        assertEquals(0, result.getTotalTiersPayant().compareTo(BigDecimal.valueOf(0.0)));
        assertEquals(0, result.getTotalPatientShare().compareTo(BigDecimal.valueOf(2000.0)));
        assertEquals(BigDecimal.ZERO, result.getDiscountAmount());
    }
}
