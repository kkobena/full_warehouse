package com.kobe.warehouse.service.dto.projection;

import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import java.math.BigDecimal;

public interface MouvementCaisse {
    default String getLibelle() {
        return getType().getValue();
    }

    BigDecimal getMontant();

    TypeFinancialTransaction getType();
}
