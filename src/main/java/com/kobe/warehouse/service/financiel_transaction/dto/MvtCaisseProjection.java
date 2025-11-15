package com.kobe.warehouse.service.financiel_transaction.dto;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MvtCaisseProjection(
    Long id,
    Long saleId,
    Integer caisseId,
    Long invoiceId,
    boolean isCredit,
    String type,
    LocalDateTime createdAt,
    Integer paidAmount,
    TypeFinancialTransaction typeFinancialTransaction,
    String paymentMode,
    String paymentModeLibelle,
    CategorieChiffreAffaire categorieChiffreAffaire,
    LocalDate transactionDate,
    Integer deliveryId,
    String firstName,
    String lastName,
    Integer montant,
    LocalDate saleDate,
    String transactionNumber
) {
    public PaymentType paymentType() {
        return PaymentType.valueOf(type);
    }
}
