package com.kobe.warehouse.service.tiketz.dto;

import com.kobe.warehouse.domain.enumeration.ModePaimentCode;

public record TicketZProjection(
    String codeModePaiment,
    String libelle,
    Long userId,
    String firstName,
    String lastName,
    Long montant,
    Long montantReel,
    boolean credit
) {
    public ModePaimentCode modePaimentCode(){
        return ModePaimentCode.valueOf(codeModePaiment);
    }
}
