package com.kobe.warehouse.service.cash_register.dto;

import com.kobe.warehouse.domain.enumeration.CashRegisterStatut;
import java.time.LocalDate;
import java.util.Set;

public record FetchCashRegisterParams(
    Integer userId,
    Set<CashRegisterStatut> statuts,
    LocalDate fromDate,
    LocalDate toDate,
    String beginTime,
    String endTime
) {}
