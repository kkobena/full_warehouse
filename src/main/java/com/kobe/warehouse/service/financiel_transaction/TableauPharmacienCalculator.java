package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.service.financiel_transaction.dto.AchatDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.FournisseurAchat;
import com.kobe.warehouse.service.financiel_transaction.dto.PaymentDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Calculator for TableauPharmacien ratios and aggregations
 */
@Component
public class TableauPharmacienCalculator {

    private static final Logger LOG = LoggerFactory.getLogger(TableauPharmacienCalculator.class);
    private static final int DECIMAL_SCALE = 2;

    /**
     * Calculate ratio Vente/Achat for wrapper
     */
    public void calculateRatioVenteAchat(TableauPharmacienWrapper wrapper) {
        long netPurchase = wrapper.getMontantAchatNet() - wrapper.getMontantAvoirFournisseur();
        if (netPurchase == 0) {
            wrapper.setRatioVenteAchat(0f);
            return;
        }

        try {
            float ratio = BigDecimal.valueOf(wrapper.getMontantVenteNet())
                .divide(BigDecimal.valueOf(netPurchase), DECIMAL_SCALE, RoundingMode.FLOOR)
                .floatValue();
            wrapper.setRatioVenteAchat(ratio);
        } catch (ArithmeticException e) {
            LOG.warn("Error calculating ratio V/A for wrapper: {}", e.getMessage());
            wrapper.setRatioVenteAchat(0f);
        }
    }

    /**
     * Calculate ratio Achat/Vente for wrapper
     */
    public void calculateRatioAchatVente(TableauPharmacienWrapper wrapper) {
        if (wrapper.getMontantVenteNet() == 0) {
            wrapper.setRatioAchatVente(0f);
            return;
        }

        try {
            long netPurchase = wrapper.getMontantAchatNet() - wrapper.getMontantAvoirFournisseur();
            float ratio = BigDecimal.valueOf(netPurchase)
                .divide(BigDecimal.valueOf(wrapper.getMontantVenteNet()), DECIMAL_SCALE, RoundingMode.FLOOR)
                .floatValue();
            wrapper.setRatioAchatVente(ratio);
        } catch (ArithmeticException e) {
            LOG.warn("Error calculating ratio A/V for wrapper: {}", e.getMessage());
            wrapper.setRatioAchatVente(0f);
        }
    }

    /**
     * Calculate ratio Vente/Achat for daily/monthly entry
     */
    public void calculateRatioVenteAchat(TableauPharmacienDTO dto) {
        long netPurchase = dto.getMontantBonAchat() - dto.getMontantAvoirFournisseur();
        if (netPurchase == 0) {
            dto.setRatioVenteAchat(0f);
            return;
        }

        try {
            float ratio = BigDecimal.valueOf(dto.getMontantNet())
                .divide(BigDecimal.valueOf(netPurchase), DECIMAL_SCALE, RoundingMode.FLOOR)
                .floatValue();
            dto.setRatioVenteAchat(ratio);
        } catch (ArithmeticException e) {
            LOG.warn("Error calculating ratio V/A for DTO: {}", e.getMessage());
            dto.setRatioVenteAchat(0f);
        }
    }

    /**
     * Calculate ratio Achat/Vente for daily/monthly entry
     */
    public void calculateRatioAchatVente(TableauPharmacienDTO dto) {
        if (dto.getMontantNet() == 0) {
            dto.setRatioAchatVente(0f);
            return;
        }

        try {
            long netPurchase = dto.getMontantBonAchat() - dto.getMontantAvoirFournisseur();
            float ratio = BigDecimal.valueOf(netPurchase)
                .divide(BigDecimal.valueOf(dto.getMontantNet()), DECIMAL_SCALE, RoundingMode.FLOOR)
                .floatValue();
            dto.setRatioAchatVente(ratio);
        } catch (ArithmeticException e) {
            LOG.warn("Error calculating ratio A/V for DTO: {}", e.getMessage());
            dto.setRatioAchatVente(0f);
        }
    }

    /**
     * Calculate payment totals for a TableauPharmacienDTO
     * Optimized to use single loop instead of multiple iterations
     */
    public void calculatePaymentTotals(TableauPharmacienDTO dto) {
        List<PaymentDTO> payments = dto.getPayments();
        if (payments == null || payments.isEmpty()) {
            return;
        }

        // Single loop to calculate both totals
        long montantReel = 0;
        long montantComptant = 0;

        for (PaymentDTO payment : payments) {
            montantReel += payment.realAmount();
            montantComptant += payment.paidAmount();
        }

        dto.setMontantReel(montantReel);
        dto.setMontantComptant(montantComptant);
    }

    /**
     * Calculate net amount considering remises
     */
    public void calculateNetAmount(TableauPharmacienDTO dto) {
        long montantNet = dto.getMontantTtc() + dto.getMontantRemise() - dto.getMontantRemiseUg();
        dto.setMontantNet(montantNet);
    }

    /**
     * Adjust cash amount for unit gratuite
     */
    public void adjustCashAmountForUnitGratuite(TableauPharmacienDTO dto) {
        long adjustedComptant = dto.getMontantComptant() - dto.getMontantTtcUg();
        dto.setMontantComptant(adjustedComptant);
    }

    /**
     * Aggregate AchatDTO amounts
     */
    public AchatDTO aggregateAchats(List<AchatDTO> achats, AchatDTO initialAchat) {
        for (AchatDTO achat : achats) {
            initialAchat.setMontantNet(initialAchat.getMontantNet() + achat.getMontantNet());
            initialAchat.setMontantTtc(initialAchat.getMontantTtc() + achat.getMontantTtc());
            initialAchat.setMontantHt(initialAchat.getMontantHt() + achat.getMontantHt());
            initialAchat.setMontantTaxe(initialAchat.getMontantTaxe() + achat.getMontantTaxe());
            initialAchat.setMontantRemise(initialAchat.getMontantRemise() + achat.getMontantRemise());
        }
        return initialAchat;
    }
}
