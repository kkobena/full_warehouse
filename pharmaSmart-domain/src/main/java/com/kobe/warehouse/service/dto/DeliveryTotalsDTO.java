package com.kobe.warehouse.service.dto;

/**
 * Totaux comptables d'une période de réceptions.
 *
 * @param count           nombre de bons de livraison
 * @param totalGrossAmount montant total brut achat (grossAmount, HT avant remises)
 * @param totalNetAmount   montant total HT net (htAmount, après remises)
 * @param totalTaxAmount   montant total TVA (taxAmount)
 */
public record DeliveryTotalsDTO(
    long count,
    long totalGrossAmount,
    long totalNetAmount,
    long totalTaxAmount
) {}
