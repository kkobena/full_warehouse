package com.kobe.warehouse.service.sale.dto;

public record RetourClientLineDTO(
    Integer id,
    String produitLibelle,
    String codeCip,
    int quantite,
    int prixUnitaire,
    int montant,
    int montantTp
) {}
