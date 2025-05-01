package com.kobe.warehouse.service.financiel_transaction.dto;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MvtCaisseProjection(
    Long id,
    Long saleId,
    Long caisseId,
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
    Long deliveryId,
    String firstName,
    String lastName,
    Integer montant
) {
    public PaymentType paymentType() {
        return PaymentType.valueOf(type);
    }
}
