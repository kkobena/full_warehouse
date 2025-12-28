package com.kobe.warehouse.service.dto.projection;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kobe.warehouse.domain.enumeration.ModePaimentCode;

import java.math.BigDecimal;

public record Recette(Long realAmount, Long paidAmount, String code, String libelle) {
    @JsonProperty("montantPaye")
    public BigDecimal getMontantPaye() {
        return BigDecimal.valueOf(paidAmount);
    }

    @JsonProperty("montantReel")
    public BigDecimal getMontantReel() {
        return BigDecimal.valueOf(realAmount);
    }

    @JsonProperty("modePaimentCode")
    public ModePaimentCode getModePaimentCode() {
        return ModePaimentCode.fromName(code);
    }

    @JsonProperty("modePaimentLibelle")
    public String getModePaimentLibelle() {
        return libelle;
    }
}
