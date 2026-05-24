package com.kobe.warehouse.service.dto.dashboard;

/**
 * Une ligne d'encaissement par mode de paiement pour la session du caissier.
 * Générée dynamiquement depuis payment_transaction GROUP BY payment_mode.
 */
public record EncaissementParModeDTO(
    String code,
    String libelle,
    String paymentGroup,
    Long montant
) {}
