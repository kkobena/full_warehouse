package com.kobe.warehouse.service.dto.ordonnance;

/** Une ligne prescrite et ce qui en a été délivré (ventes clôturées et non annulées seulement). */
public record LigneSuivieDTO(int quantitePrescrite, int quantiteDelivree) {}
