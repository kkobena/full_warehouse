package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for CA by product family
 */
public record ProductFamilyCADTO(
    LocalDate saleDate,
    String famille,
    Integer quantiteVendue,
    Long caTotal,
    Long coutTotal,
    Long margeBrute,
    BigDecimal tauxMargePct,
    Integer nbLignesVente
) {}
