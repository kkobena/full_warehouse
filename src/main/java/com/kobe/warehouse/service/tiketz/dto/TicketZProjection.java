package com.kobe.warehouse.service.tiketz.dto;

import com.kobe.warehouse.domain.enumeration.ModePaimentCode;

public record TicketZProjection(
    ModePaimentCode modePaimentCode,
    String libelle,
    Long userId,
    String firstName,
    String lastName,
    Long montant,
    Long montantReel,
    boolean credit
) {}
