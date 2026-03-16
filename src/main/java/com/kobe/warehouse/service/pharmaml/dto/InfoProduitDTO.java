package com.kobe.warehouse.service.pharmaml.dto;

public record InfoProduitDTO(
    String codeProduit,
    String designation,
    int stockDisponible,
    int prixAchat,
    boolean disponible
) {}
