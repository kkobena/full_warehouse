package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;

/**
 * Résumé global des marges, sans distribution BCG.
 * Agrège les données de mv_product_profitability avec filtres optionnels famille/rayon.
 *
 * @param totalProduits             nombre total de produits
 * @param caTotalGlobal             CA total sur la période
 * @param coutAchatGlobal           coût d'achat total
 * @param margeBruteGlobale         marge brute globale
 * @param tauxMargeMoyen            taux de marge moyen pondéré par le CA
 * @param nbProduitsMargeInsuffisante nombre de produits à marge < seuil bas (défaut 10 %)
 * @param caProduitsFaibleMarge     CA réalisé par les produits à marge insuffisante
 * @param nbProduitsMargeConfortable  nombre de produits à marge >= seuil haut (défaut 20 %)
 * @param caProduitsBonneMarge      CA réalisé par les produits à bonne marge
 */
public record MargeSummaryDTO(
    Integer totalProduits,
    Long caTotalGlobal,
    Long coutAchatGlobal,
    Long margeBruteGlobale,
    BigDecimal tauxMargeMoyen,
    Integer nbProduitsMargeInsuffisante,
    Long caProduitsFaibleMarge,
    Integer nbProduitsMargeConfortable,
    Long caProduitsBonneMarge
) {}

