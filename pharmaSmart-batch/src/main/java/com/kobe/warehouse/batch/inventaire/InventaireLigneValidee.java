package com.kobe.warehouse.batch.inventaire;

import java.time.LocalDate;

/**
 * Ligne d'inventaire validée, prête à être persistée.
 */
public record InventaireLigneValidee(String cip13, String lot, int quantite, LocalDate peremption) {}
