package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;

/**
 * Achats aupres d'un fournisseur sur une fenetre quelconque.
 *
 * <p>{@link SupplierPerformanceDTO} ne porte que deux fenetres, 30 jours et 12 mois, figees dans
 * {@code mv_supplier_performance} : impossible d'y suivre le selecteur de periode du tableau de
 * bord. Le montant et le nombre de commandes sont donc calcules en direct sur {@code commande},
 * tandis que le score et le delai restent ceux de la vue -- ce sont des indicateurs de qualite
 * sur douze mois, qu'une fenetre d'un jour ne saurait mesurer.
 */
public record SupplierPurchaseDTO(
    Integer fournisseurId,
    String fournisseurName,
    Integer nbCommandes,
    Long montantAchat,
    Integer avgDeliveryDays,
    BigDecimal performanceScore
) {}
