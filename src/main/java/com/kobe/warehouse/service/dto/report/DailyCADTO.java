package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for daily Chiffre d'Affaires (CA) summary
 */
public record DailyCADTO(
    LocalDate saleDate,
    Integer nbTransactions,
    Integer nbAvoirs,
    Long caTotal,
    Long caAvoirs,
    Long caNet,
    BigDecimal panierMoyen,
    Long coutTotal,
    Long margeBrute,
    BigDecimal tauxMargePct,
    Integer nbClients,
    Long montantEncaisse,
    Long montantCredit
) {}
