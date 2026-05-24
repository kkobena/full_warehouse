package com.kobe.warehouse.service.dto.projection;

import java.math.BigDecimal;

public interface GroupeFournisseurAchat {
    String getLibelle();

    BigDecimal getMontantHt();

    BigDecimal getMontantTtc();

    BigDecimal getMontantTva();
}
