package com.kobe.warehouse.service.dto.ordonnance;

public record OrdonnanceLigneDTO(
    Integer id,
    int rang,
    String texteLu,
    Integer produitId,
    String produitLibelle,
    String posologie,
    Integer dureeJours,
    int quantitePrescrite,
    int quantiteDelivree,
    int resteADelivrer
) {}
