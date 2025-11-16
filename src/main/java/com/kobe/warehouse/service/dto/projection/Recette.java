package com.kobe.warehouse.service.dto.projection;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import java.math.BigDecimal;

public record Recette(String code, String libelle, BigDecimal paidAmount, BigDecimal realAmount) {
    @JsonProperty("montantPaye")
    public BigDecimal getMontantPaye() {
        return paidAmount;
    }

    @JsonProperty("montantReel")
    public BigDecimal getMontantReel() {
        return realAmount;
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
