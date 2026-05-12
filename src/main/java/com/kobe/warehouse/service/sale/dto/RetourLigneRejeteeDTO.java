package com.kobe.warehouse.service.sale.dto;

import com.kobe.warehouse.domain.enumeration.StatutLegal;

public record RetourLigneRejeteeDTO(
    String produitLibelle,
    String codeCip,
    int quantite,
    StatutLegal statutLegal,
    String raison
) {}
