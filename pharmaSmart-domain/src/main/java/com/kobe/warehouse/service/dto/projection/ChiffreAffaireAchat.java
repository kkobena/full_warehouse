package com.kobe.warehouse.service.dto.projection;

import java.math.BigDecimal;

public interface ChiffreAffaireAchat {
    BigDecimal getMontantTtc();

    BigDecimal getMontantTva();

    BigDecimal getMontantHt();
}
