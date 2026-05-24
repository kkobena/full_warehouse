package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;

/**
 * DTO allégé de profitabilité produit, sans classification BCG.
 * Basé sur mv_product_profitability, filtrable par famille et rayon.
 */
public record MargeDTO(
    Integer produitId,
    String libelle,
    String codeCip,
    String categorie,
    Integer nbVentes,
    Integer qteVendue,
    Long caTotal,
    Long coutAchatTotal,
    Long margeBrute,
    BigDecimal tauxMargePct,
    Integer prixVenteMoyen,
    Integer prixAchatMoyen,
    Integer stockQuantity,
    Integer prixAchatUnitaire,
    Integer prixVenteUnitaire,
    BigDecimal tauxRotationAnnuel
) {}

