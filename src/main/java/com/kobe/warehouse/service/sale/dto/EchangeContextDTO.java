package com.kobe.warehouse.service.sale.dto;

import java.util.List;

/**
 * Contexte d'échange retourné après un retour avec échange.
 * Le frontend l'utilise pour ouvrir la caisse pré-remplie avec le client
 * et le crédit disponible.
 */
public record EchangeContextDTO(
    Integer customerId,
    String customerName,
    int montantCredit,
    Integer retourId,
    String retourReference,
    List<String> avoirReferences
) {}
