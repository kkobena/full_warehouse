package com.kobe.warehouse.service.dto.projection;

import java.math.BigDecimal;
import java.util.Objects;

public interface ChiffreAffaire {
    BigDecimal getMontantTtc();

    BigDecimal getMontantTva();

    BigDecimal getMontantHt();

    BigDecimal getMontantRemise();

    BigDecimal getMontantNet();

    default BigDecimal getMontantCredit() {
        return Objects.requireNonNullElse(getMontantTp(), BigDecimal.ZERO).add(
            Objects.requireNonNullElse(getMontantDiffere(), BigDecimal.ZERO)
        );
    }

    BigDecimal getMontantTp();

    BigDecimal getMontantDiffere();

    BigDecimal getMontantAchat();

    default BigDecimal getMarge() {
        return Objects.requireNonNullElse(getMontantTtc(), BigDecimal.ZERO).subtract(
            Objects.requireNonNullElse(getMontantAchat(), BigDecimal.ZERO)
        );
    }
}
