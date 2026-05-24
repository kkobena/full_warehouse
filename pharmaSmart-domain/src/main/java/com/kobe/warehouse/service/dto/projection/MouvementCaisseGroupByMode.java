package com.kobe.warehouse.service.dto.projection;

import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import java.math.BigDecimal;

public interface MouvementCaisseGroupByMode {
    default String getLibelle() {
        return getType().getValue();
    }

    BigDecimal getMontant();

    TypeFinancialTransaction getType();

    ModePaimentCode getModePaimentCode();

    String getModePaimentLibelle();
}
