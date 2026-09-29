package com.kobe.warehouse.service.customer;

/**
 * Situation d'un client face à la limite de crédit.
 *
 * @param limite     limite de l'officine ; 0 = aucune
 * @param encours    reste dû sur les ventes différées
 * @param disponible marge avant la limite ; {@code null} sans limite
 */
public record SituationCreditDTO(int limite, long encours, Long disponible) {}
