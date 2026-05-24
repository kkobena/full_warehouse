package com.kobe.warehouse.service.financiel_transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kobe.warehouse.service.financiel_transaction.dto.AchatDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.PaymentDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienWrapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TableauPharmacienCalculatorTest {

    private TableauPharmacienCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new TableauPharmacienCalculator();
    }

    // ===== Ratio Calculations Tests =====

    @Test
    void testCalculateRatioVenteAchat_wrapper_normal() {
        TableauPharmacienWrapper wrapper = new TableauPharmacienWrapper();
        wrapper.setMontantVenteNet(10000L);
        wrapper.setMontantAchatNet(8000L);
        wrapper.setMontantAvoirFournisseur(1000L);

        calculator.calculateRatioVenteAchat(wrapper);

        // 10000 / (8000 - 1000) = 10000 / 7000 = 1.42
        assertEquals(1.42f, wrapper.getRatioVenteAchat(), 0.01f);
    }

    @Test
    void testCalculateRatioVenteAchat_wrapper_zeroPurchase() {
        TableauPharmacienWrapper wrapper = new TableauPharmacienWrapper();
        wrapper.setMontantVenteNet(10000L);
        wrapper.setMontantAchatNet(0L);
        wrapper.setMontantAvoirFournisseur(0L);

        calculator.calculateRatioVenteAchat(wrapper);

        assertEquals(0f, wrapper.getRatioVenteAchat());
    }

    @Test
    void testCalculateRatioVenteAchat_wrapper_purchaseEqualsAvoirs() {
        TableauPharmacienWrapper wrapper = new TableauPharmacienWrapper();
        wrapper.setMontantVenteNet(10000L);
        wrapper.setMontantAchatNet(5000L);
        wrapper.setMontantAvoirFournisseur(5000L);

        calculator.calculateRatioVenteAchat(wrapper);

        // Net purchase = 0, so ratio = 0
        assertEquals(0f, wrapper.getRatioVenteAchat());
    }

    @Test
    void testCalculateRatioAchatVente_wrapper_normal() {
        TableauPharmacienWrapper wrapper = new TableauPharmacienWrapper();
        wrapper.setMontantVenteNet(10000L);
        wrapper.setMontantAchatNet(8000L);
        wrapper.setMontantAvoirFournisseur(1000L);

        calculator.calculateRatioAchatVente(wrapper);

        // (8000 - 1000) / 10000 = 7000 / 10000 = 0.70
        assertEquals(0.70f, wrapper.getRatioAchatVente(), 0.01f);
    }

    @Test
    void testCalculateRatioAchatVente_wrapper_zeroSales() {
        TableauPharmacienWrapper wrapper = new TableauPharmacienWrapper();
        wrapper.setMontantVenteNet(0L);
        wrapper.setMontantAchatNet(5000L);
        wrapper.setMontantAvoirFournisseur(0L);

        calculator.calculateRatioAchatVente(wrapper);

        assertEquals(0f, wrapper.getRatioAchatVente());
    }

    @Test
    void testCalculateRatioVenteAchat_dto_normal() {
        TableauPharmacienDTO dto = new TableauPharmacienDTO();
        dto.setMontantNet(5000L);
        dto.setMontantBonAchat(4000L);
        dto.setMontantAvoirFournisseur(500L);

        calculator.calculateRatioVenteAchat(dto);

        // 5000 / (4000 - 500) = 5000 / 3500 = 1.42
        assertEquals(1.42f, dto.getRatioVenteAchat(), 0.01f);
    }

    @Test
    void testCalculateRatioAchatVente_dto_normal() {
        TableauPharmacienDTO dto = new TableauPharmacienDTO();
        dto.setMontantNet(5000L);
        dto.setMontantBonAchat(4000L);
        dto.setMontantAvoirFournisseur(500L);

        calculator.calculateRatioAchatVente(dto);

        // (4000 - 500) / 5000 = 3500 / 5000 = 0.70
        assertEquals(0.70f, dto.getRatioAchatVente(), 0.01f);
    }

    // ===== Payment Totals Tests =====

    @Test
    void testCalculatePaymentTotals_withPayments() {
        TableauPharmacienDTO dto = new TableauPharmacienDTO();
        List<PaymentDTO> payments = new ArrayList<>();
        // PaymentDTO(String code, String libelle, Long paidAmount, Long realAmount)
        payments.add(new PaymentDTO("CASH", "Cash Payment", 900L, 1000L));
        payments.add(new PaymentDTO("CARD", "Card Payment", 1800L, 2000L));
        payments.add(new PaymentDTO("CHECK", "Check Payment", 1500L, 1500L));
        dto.setPayments(payments);

        calculator.calculatePaymentTotals(dto);

        // Real amount = 1000 + 2000 + 1500 = 4500
        // Paid amount = 900 + 1800 + 1500 = 4200
        assertEquals(4500L, dto.getMontantReel());
        assertEquals(4200L, dto.getMontantComptant());
    }

    @Test
    void testCalculatePaymentTotals_emptyPayments() {
        TableauPharmacienDTO dto = new TableauPharmacienDTO();
        dto.setPayments(new ArrayList<>());

        calculator.calculatePaymentTotals(dto);

        // Should not throw, fields remain unset
        assertEquals(0L, dto.getMontantReel());
        assertEquals(0L, dto.getMontantComptant());
    }

    @Test
    void testCalculatePaymentTotals_nullPayments() {
        TableauPharmacienDTO dto = new TableauPharmacienDTO();
        dto.setPayments(null);

        calculator.calculatePaymentTotals(dto);

        // Should not throw
        assertEquals(0L, dto.getMontantReel());
        assertEquals(0L, dto.getMontantComptant());
    }

    // ===== Net Amount Calculation Tests =====

    @Test
    void testCalculateNetAmount_normal() {
        TableauPharmacienDTO dto = new TableauPharmacienDTO();
        dto.setMontantTtc(10000L);
        dto.setMontantRemise(500L);
        dto.setMontantRemiseUg(200L);

        calculator.calculateNetAmount(dto);

        // 10000 + 500 - 200 = 10300
        assertEquals(10300L, dto.getMontantNet());
    }

    @Test
    void testCalculateNetAmount_noRemises() {
        TableauPharmacienDTO dto = new TableauPharmacienDTO();
        dto.setMontantTtc(10000L);
        dto.setMontantRemise(0L);
        dto.setMontantRemiseUg(0L);

        calculator.calculateNetAmount(dto);

        assertEquals(10000L, dto.getMontantNet());
    }

    // ===== Cash Adjustment Tests =====

    @Test
    void testAdjustCashAmountForUnitGratuite_normal() {
        TableauPharmacienDTO dto = new TableauPharmacienDTO();
        dto.setMontantComptant(10000L);
        dto.setMontantTtcUg(500L);

        calculator.adjustCashAmountForUnitGratuite(dto);

        // 10000 - 500 = 9500
        assertEquals(9500L, dto.getMontantComptant());
    }

    @Test
    void testAdjustCashAmountForUnitGratuite_noUnitGratuite() {
        TableauPharmacienDTO dto = new TableauPharmacienDTO();
        dto.setMontantComptant(10000L);
        dto.setMontantTtcUg(0L);

        calculator.adjustCashAmountForUnitGratuite(dto);

        assertEquals(10000L, dto.getMontantComptant());
    }

    // ===== Achat Aggregation Tests =====

    @Test
    void testAggregateAchats_multipleAchats() {
        AchatDTO initial = new AchatDTO();

        List<AchatDTO> achats = new ArrayList<>();
        AchatDTO achat1 = createAchatDTO(1000L, 1100L, 900L, 100L, 50L);
        AchatDTO achat2 = createAchatDTO(2000L, 2200L, 1800L, 200L, 100L);
        AchatDTO achat3 = createAchatDTO(1500L, 1650L, 1350L, 150L, 75L);
        achats.add(achat1);
        achats.add(achat2);
        achats.add(achat3);

        AchatDTO result = calculator.aggregateAchats(achats, initial);

        assertEquals(4500L, result.getMontantNet());
        assertEquals(4950L, result.getMontantTtc());
        assertEquals(4050L, result.getMontantHt());
        assertEquals(450L, result.getMontantTaxe());
        assertEquals(225L, result.getMontantRemise());
    }

    @Test
    void testAggregateAchats_emptyList() {
        AchatDTO initial = createAchatDTO(1000L, 1100L, 900L, 100L, 50L);

        AchatDTO result = calculator.aggregateAchats(new ArrayList<>(), initial);

        // Should return unchanged initial
        assertEquals(1000L, result.getMontantNet());
        assertEquals(1100L, result.getMontantTtc());
    }

    @Test
    void testAggregateAchats_withExistingAmounts() {
        AchatDTO initial = createAchatDTO(1000L, 1100L, 900L, 100L, 50L);

        List<AchatDTO> achats = new ArrayList<>();
        achats.add(createAchatDTO(500L, 550L, 450L, 50L, 25L));

        AchatDTO result = calculator.aggregateAchats(achats, initial);

        // Should add to existing amounts
        assertEquals(1500L, result.getMontantNet());
        assertEquals(1650L, result.getMontantTtc());
        assertEquals(1350L, result.getMontantHt());
        assertEquals(150L, result.getMontantTaxe());
        assertEquals(75L, result.getMontantRemise());
    }

    // ===== Helper Methods =====

    private AchatDTO createAchatDTO(long net, long ttc, long ht, long taxe, long remise) {
        AchatDTO achat = new AchatDTO();
        achat.setMontantNet(net);
        achat.setMontantTtc(ttc);
        achat.setMontantHt(ht);
        achat.setMontantTaxe(taxe);
        achat.setMontantRemise(remise);
        return achat;
    }
}
