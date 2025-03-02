package com.kobe.warehouse.service.dto.filter;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.service.dto.enumeration.Order;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record TransactionFilterDTO(
    LocalDate fromDate,
    LocalDate toDate,
    Long userId,
    String search,
    Set<TypeFinancialTransaction> typeFinancialTransactions,
    Set<CategorieChiffreAffaire> categorieChiffreAffaires,
    Set<String> paymentModes,
    Order order,
    LocalTime fromTime,
    LocalTime toTime
) {}
