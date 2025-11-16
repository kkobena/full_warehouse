package com.kobe.warehouse.service.dto.projection;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record ChiffreAffaire(
    BigDecimal montantTtc,
    BigDecimal montantHt,
    BigDecimal montantRemise,
    BigDecimal montantDiffere,
    BigDecimal montantTp,
    BigDecimal montantAchat,
    BigDecimal montantRemiseUg,
    List<Recette> payments
) {
    @JsonProperty("montantTva")
    public BigDecimal getMontantTva() {
        return Objects.requireNonNullElse(montantTtc, BigDecimal.ZERO).subtract(Objects.requireNonNullElse(montantHt, BigDecimal.ZERO));
    }

    @JsonProperty("montantCredit")
    public BigDecimal getMontantCredit() {
        return Objects.requireNonNullElse(montantTp, BigDecimal.ZERO).add(Objects.requireNonNullElse(montantDiffere, BigDecimal.ZERO));
    }

    public BigDecimal getMarge() {
        return Objects.requireNonNullElse(montantHt, BigDecimal.ZERO).subtract(Objects.requireNonNullElse(montantAchat, BigDecimal.ZERO));
    }

    @JsonProperty("montantNet")
    public BigDecimal montantNet() {
        return Objects.requireNonNullElse(montantTtc, BigDecimal.ZERO).subtract(Objects.requireNonNullElse(montantRemise, BigDecimal.ZERO));
    }
}
