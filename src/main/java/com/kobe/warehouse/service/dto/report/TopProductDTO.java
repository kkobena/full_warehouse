package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for top products report from mv_monthly_top_products materialized view
 */
public record TopProductDTO(
    LocalDate mois,
    Integer produitId,
    String libelle,
    String codeCip,
    Long nbVentes,
    Integer qteVendue,
    Integer caGenere,
    BigDecimal prixMoyen
) {}
