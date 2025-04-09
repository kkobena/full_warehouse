package com.kobe.warehouse.service.dto;

public record EtatProduit(
    boolean stockPositif,
    boolean sockZero,
    boolean stockNegatif,
    boolean enSuggestion,
    boolean enCommande,
    boolean entree
) {}
