package com.kobe.warehouse.domain;

import java.time.LocalDateTime;

public record HistoriqueProduitInventaire(LocalDateTime dateInventaire, int quantiteInventaire,
                                          int quantiteInit, long idInventaire) {
}
