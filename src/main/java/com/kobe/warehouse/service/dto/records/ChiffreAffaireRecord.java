package com.kobe.warehouse.service.dto.records;

import java.math.BigDecimal;

public record ChiffreAffaireRecord(
    BigDecimal montantTtc,
    BigDecimal montantTva,
    BigDecimal montantHt,
    BigDecimal montantRemise,
    BigDecimal montantNet,
    BigDecimal montantEspece,
    BigDecimal montantCredit,
    BigDecimal montantRegle,
    BigDecimal montantAutreModePaiement,
    BigDecimal marge
) {}
