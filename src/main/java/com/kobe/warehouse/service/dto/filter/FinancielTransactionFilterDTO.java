package com.kobe.warehouse.service.dto.filter;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import java.time.LocalDate;

public record FinancielTransactionFilterDTO(
    LocalDate fromDate,
    LocalDate toDate,
    Long userId,
    String search,
    TypeFinancialTransaction typeFinancialTransaction,
    CategorieChiffreAffaire categorieChiffreAffaire,
    String paymentMode,
    String organismeId) {}
