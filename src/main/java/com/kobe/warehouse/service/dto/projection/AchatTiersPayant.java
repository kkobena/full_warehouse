package com.kobe.warehouse.service.dto.projection;

import java.math.BigDecimal;

public interface AchatTiersPayant {
    String getLibelle();

    String getCategorie();

    Integer getClientCount();

    BigDecimal getMontant();

    Integer getBonsCount();
}
