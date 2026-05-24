package com.kobe.warehouse.service.dto.records;

/**
 * Agrégat des écarts par cause pour un inventaire clôturé.
 *
 * @param cause         code de la cause (ex: "VOL")
 * @param causeLabel    libellé lisible
 * @param nbProduits    nombre de produits concernés
 * @param quantiteTotale quantité totale en écart pour cette cause
 */
public record GapSummaryRecord(
    String cause,
    String causeLabel,
    long nbProduits,
    long quantiteTotale
) {}
