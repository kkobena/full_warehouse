package com.kobe.warehouse.service.dto.projection;

import java.math.BigDecimal;

public interface MouvementCaisse {
    String getLibelle();

    BigDecimal getMontant();
}
