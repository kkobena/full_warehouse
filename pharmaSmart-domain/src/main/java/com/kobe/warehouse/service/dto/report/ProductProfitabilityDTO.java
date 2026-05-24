package com.kobe.warehouse.service.dto.report;

import com.kobe.warehouse.domain.enumeration.BCGCategory;
import java.math.BigDecimal;

/**
 * DTO for product profitability report from mv_product_profitability materialized view
 * Includes margin analysis and BCG Matrix classification
 */
public record ProductProfitabilityDTO(
    Integer produitId,
    String libelle,
    String codeCip,
    String categorie,
    Integer nbVentes,
    Integer qteVendue,
    Integer caTotal,
    Integer coutAchatTotal,
    Integer margeBrute,
    BigDecimal tauxMargePct,
    Integer prixVenteMoyen,
    Integer prixAchatMoyen,
    Integer stockQuantity,
    Integer prixAchatUnitaire,
    Integer prixVenteUnitaire,
    BigDecimal tauxRotationAnnuel,
    BCGCategory bcgCategory
) {}
