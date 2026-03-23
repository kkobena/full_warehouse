package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import java.math.BigDecimal;

/**
 * Indicateurs analytiques d'un produit pour le panneau détail de la fiche article.
 *
 * <p>Agrège les données de {@code v_stock_rotation} et {@code v_abc_pareto_analysis}
 * en un seul objet léger, chargé à la sélection d'un produit (lazy load).
 */
public record ProduitIndicateursDTO(
    Integer produitId,

    // Classification
    ClasseCriticite classeCriticite,
    boolean estMedicamentEssentiel,
    boolean estProduitGarde,

    // Rotation (depuis v_stock_rotation)
    BigDecimal cmm,
    BigDecimal rotationAnnuelleQte,
    Integer couvertureStockJours,

    // Chiffre d'affaires (depuis v_stock_rotation)
    Integer ca30Jours,
    Integer ca12Mois,
    Integer qteVendue12Mois,

    // Marge (calculée : (PV - PA) / PV * 100)
    BigDecimal tauxMarge,

    // Pareto (depuis v_abc_pareto_analysis)
    Integer rang,
    BigDecimal caCumulePct,
    Integer frequenceMois
) {}
