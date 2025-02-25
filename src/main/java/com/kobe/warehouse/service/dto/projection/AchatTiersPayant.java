package com.kobe.warehouse.service.dto.projection;

import java.math.BigDecimal;

public interface AchatTiersPayant {
    String getLibelle();

    String getType();

    Integer getAchatCount();

    BigDecimal getMontant();

    Integer getBonsCount();
}
