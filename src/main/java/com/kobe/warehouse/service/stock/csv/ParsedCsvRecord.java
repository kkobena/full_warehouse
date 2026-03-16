package com.kobe.warehouse.service.stock.csv;

import java.time.LocalDate;

/**
 * Données extraites d'une ligne d'un fichier CSV fournisseur lors de l'import d'une commande.
 */
public record ParsedCsvRecord(
    String codeProduit,
    int quantityRequested,
    int quantityReceived,
    int orderCostAmount,
    int orderUnitPrice,
    int quantityUg,
    int taxAmount,
    String lotNumber,
    LocalDate expirationDate
) {}
