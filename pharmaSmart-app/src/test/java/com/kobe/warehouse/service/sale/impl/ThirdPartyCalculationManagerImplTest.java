package com.kobe.warehouse.service.sale.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.OptionPrixProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.Tva;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.OptionPrixType;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.domain.enumeration.TypeVente;
import com.kobe.warehouse.repository.ThirdPartySaleRepository;
import com.kobe.warehouse.service.produit_prix.service.PrixRererenceService;
import com.kobe.warehouse.service.sale.SalesLineService;
import com.kobe.warehouse.service.sale.calculation.TiersPayantCalculationService;
import com.kobe.warehouse.service.sale.calculation.dto.CalculatedShare;
import com.kobe.warehouse.service.sale.calculation.dto.CalculationInput;
import com.kobe.warehouse.service.sale.calculation.dto.CalculationResult;
import com.kobe.warehouse.service.sale.calculation.dto.Rate;
import com.kobe.warehouse.service.sale.calculation.dto.TiersPayantLineOutput;
import com.kobe.warehouse.service.sale.calculation.dto.TvaRepartitionDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ThirdPartyCalculationManagerImplTest {

    @Mock
    private TiersPayantCalculationService calculationService;
    @Mock
    private ThirdPartySaleLineService thirdPartySaleLineService;
    @Mock
    private SalesLineService salesLineService;
    @Mock
    private SaleLineServiceFactory saleLineServiceFactory;
    @Mock
    private ThirdPartySaleRepository thirdPartySaleRepository;
    @Mock
    private PrixRererenceService prixRererenceService;
    @Mock
    private SaleCommonService saleCommonService;

    private ThirdPartyCalculationManagerImpl manager;
    private ThirdPartySales sale;
    private SalesLine salesLine;
    private ThirdPartySaleLine thirdPartySaleLine;

    @BeforeEach
    void setUp() {
        when(saleLineServiceFactory.getService(TypeVente.ThirdPartySales))
            .thenReturn(salesLineService);
        manager = new ThirdPartyCalculationManagerImpl(
            calculationService,
            thirdPartySaleLineService,
            saleLineServiceFactory,
            thirdPartySaleRepository,
            prixRererenceService,
            saleCommonService);

        TiersPayant tiersPayant = new TiersPayant();
        tiersPayant.setId(30);
        tiersPayant.setFullName("Mutuelle test");

        ClientTiersPayant clientTiersPayant = new ClientTiersPayant();
        clientTiersPayant.setId(20);
        clientTiersPayant.setTiersPayant(tiersPayant);
        clientTiersPayant.setTaux(80);
        clientTiersPayant.setPriorite(PrioriteTiersPayant.R0);

        thirdPartySaleLine = new ThirdPartySaleLine();
        thirdPartySaleLine.setClientTiersPayant(clientTiersPayant);
        thirdPartySaleLine.setNumBon("BON-1");
        thirdPartySaleLine.setTauxVente((short) 75);

        Tva tva = new Tva();
        tva.setTaux(18);
        Produit produit = new Produit();
        produit.setId(40);
        produit.setTva(tva);

        salesLine = new SalesLine();
        salesLine.setId(10L);
        salesLine.setSaleDate(LocalDate.now());
        salesLine.setProduit(produit);
        salesLine.setSalesAmount(1_000);
        salesLine.setRegularUnitPrice(500);
        salesLine.setQuantityRequested(2);

        sale = new ThirdPartySales();
        sale.setNatureVente(NatureVente.ASSURANCE);
        sale.setDiscountAmount(0);
        sale.setPartAssure(1_000);
        sale.setPartTiersPayant(0);
        sale.setAmountToBePaid(1_000);
        sale.setSalesLines(new HashSet<>(List.of(salesLine)));
        sale.setThirdPartySaleLines(new ArrayList<>(List.of(thirdPartySaleLine)));
        salesLine.setSales(sale);
        thirdPartySaleLine.setSale(sale);

        lenient().when(prixRererenceService.findByProduitIdAndTiersPayantIds(eq(40), anySet()))
            .thenReturn(List.of());
    }

    @Test
    void reComputeAndApplyAmounts_ResultatNull_RemiseLesMontantsAZero() {
        when(calculationService.calculate(any(CalculationInput.class))).thenReturn(null);

        String warning = manager.reComputeAndApplyAmounts(sale, null, true);

        assertNull(warning);
        assertEquals(0, sale.getPartTiersPayant());
        assertEquals(0, sale.getPartAssure());
        assertEquals(0, sale.getAmountToBePaid());
        verify(thirdPartySaleRepository, never()).saveAndFlush(any());

        ArgumentCaptor<CalculationInput> inputCaptor = ArgumentCaptor.forClass(CalculationInput.class);
        verify(calculationService).calculate(inputCaptor.capture());
        CalculationInput input = inputCaptor.getValue();
        assertEquals(new BigDecimal("1000"), input.getTotalSalesAmount());
        assertEquals(1, input.getSaleItems().size());
        assertEquals(0.75f, input.getTiersPayants().getFirst().getTaux());
    }

    @Test
    void reComputeAndApplyAmounts_AppliqueEtPersisteTousLesResultats() {
        CalculationResult output = new CalculationResult();
        output.setTotalTiersPayant(new BigDecimal("800"));
        output.setTotalPatientShare(new BigDecimal("200"));
        output.setWarningMessage("plafond atteint");

        TvaRepartitionDto repartition = new TvaRepartitionDto(18);
        repartition.setMontantTtc(new BigDecimal("800"));
        repartition.setMontantHt(new BigDecimal("678"));
        repartition.setMontantTva(new BigDecimal("122"));
        repartition.setMontantNet(new BigDecimal("800"));
        TiersPayantLineOutput lineOutput = new TiersPayantLineOutput();
        lineOutput.setClientTiersPayantId(20);
        lineOutput.setMontant(new BigDecimal("800"));
        lineOutput.setFinalTaux(80);
        lineOutput.setRepartitions(List.of(repartition));
        output.setTiersPayantLines(List.of(lineOutput));

        CalculatedShare itemShare = new CalculatedShare();
        itemShare.setSaleLineId(10L);
        itemShare.setCalculationBasePrice(450);
        itemShare.setRates(List.of(new Rate(20, 0.8f)));
        output.setItemShares(List.of(itemShare));

        when(calculationService.calculate(any(CalculationInput.class))).thenReturn(output);
        when(saleCommonService.roundedAmount(200)).thenReturn(200);

        String warning = manager.reComputeAndApplyAmounts(sale, null, true);

        assertEquals("plafond atteint", warning);
        assertEquals(800, sale.getPartTiersPayant());
        assertEquals(200, sale.getPartAssure());
        assertEquals(200, sale.getAmountToBePaid());
        assertTrue(sale.isHasPriceOption());
        assertEquals(800, thirdPartySaleLine.getMontant());
        assertEquals(80, thirdPartySaleLine.getTaux());
        assertEquals(1, thirdPartySaleLine.getRepartitions().size());
        assertEquals(450, salesLine.getCalculationBasePrice());
        assertFalse(salesLine.getRates().isEmpty());
        verify(thirdPartySaleLineService).save(thirdPartySaleLine);
        verify(salesLineService).saveSalesLine(salesLine);
        verify(thirdPartySaleRepository).saveAndFlush(sale);
    }

    @Test
    void reComputeAndApplyAmounts_SansMiseAJour_NePersistePas() {
        CalculationResult output = new CalculationResult();
        output.setTotalTiersPayant(BigDecimal.ZERO);
        output.setTotalPatientShare(new BigDecimal("1000"));
        output.setWarningMessage(null);
        when(calculationService.calculate(any(CalculationInput.class))).thenReturn(output);
        when(saleCommonService.roundedAmount(1_000)).thenReturn(1_000);

        manager.reComputeAndApplyAmounts(sale, null, false);

        verify(thirdPartySaleLineService, never()).save(any());
        verify(salesLineService, never()).saveSalesLine(any());
        verify(thirdPartySaleRepository, never()).saveAndFlush(any());
    }

    @Test
    void computeThirdPartySaleAmounts_RecalculeLesMontantsAvantRepartition() {
        when(calculationService.calculate(any(CalculationInput.class))).thenReturn(null);

        manager.computeThirdPartySaleAmounts(sale);

        verify(saleCommonService).computeSaleEagerAmount(sale);
        verify(saleCommonService).updateAmounts(sale);
    }

    @Test
    void upddateSaleAmountsOnRemovingItem_RecalculePuisApplique() {
        when(calculationService.calculate(any(CalculationInput.class))).thenReturn(null);

        manager.upddateSaleAmountsOnRemovingItem(sale);

        verify(saleCommonService).computeSaleEagerAmount(sale);
        verify(calculationService).calculate(any(CalculationInput.class));
    }

    @Test
    void buildsOptionalLimitsAndInsuranceReferencePrice() {
        ClientTiersPayant client = thirdPartySaleLine.getClientTiersPayant();
        client.setConsoMensuelle(1_500);
        TiersPayant tiersPayant = client.getTiersPayant();
        tiersPayant.setPlafondConsoClient(20_000);
        tiersPayant.setPlafondJournalierClient(5_000);
        OptionPrixProduit reference = new OptionPrixProduit();
        reference.setTiersPayant(tiersPayant);
        reference.setPrice(450);
        reference.setRate(0.9f);
        reference.setType(OptionPrixType.REFERENCE);
        when(prixRererenceService.findByProduitIdAndTiersPayantIds(eq(40), anySet()))
            .thenReturn(List.of(reference));
        when(calculationService.calculate(any(CalculationInput.class))).thenReturn(null);

        manager.reComputeAndApplyAmounts(sale, null, false);

        ArgumentCaptor<CalculationInput> captor = ArgumentCaptor.forClass(CalculationInput.class);
        verify(calculationService).calculate(captor.capture());
        var tiersPayantInput = captor.getValue().getTiersPayants().getFirst();
        assertEquals(new BigDecimal("20000"), tiersPayantInput.getPlafondConso());
        assertEquals(new BigDecimal("1500"), tiersPayantInput.getConsoMensuelle());
        assertEquals(new BigDecimal("5000"), tiersPayantInput.getPlafondJournalierClient());
        var insurancePrice = captor.getValue().getSaleItems().getFirst().getPrixAssurances().getFirst();
        assertEquals(20, insurancePrice.getCompteTiersPayantId());
        assertEquals(450, insurancePrice.getPrice());
        assertEquals(0.9f, insurancePrice.getRate());
        assertEquals(OptionPrixType.REFERENCE, insurancePrice.getOptionPrixType());
    }

    @Test
    void buildsCalculationInputWithoutThirdPartyPayer() {
        sale.setThirdPartySaleLines(new ArrayList<>());
        when(calculationService.calculate(any(CalculationInput.class))).thenReturn(null);

        manager.reComputeAndApplyAmounts(sale, List.of(), false);

        ArgumentCaptor<CalculationInput> captor = ArgumentCaptor.forClass(CalculationInput.class);
        verify(calculationService).calculate(captor.capture());
        assertTrue(captor.getValue().getTiersPayants().isEmpty());
    }
}

