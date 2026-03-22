package com.kobe.warehouse.service.dto;

import java.time.LocalDateTime;

/**
 * DTO représentant une entrée dans l'historique des prix d'achat/vente
 * d'un produit fournisseur (table fournisseur_produit_price_history).
 */
public record PriceHistoryDTO(
    Integer id,
    Integer oldPrixAchat,
    Integer newPrixAchat,
    Integer oldPrixUni,
    Integer newPrixUni,
    LocalDateTime changedAt,
    String receiptReference,
    String changedBy
) {}
