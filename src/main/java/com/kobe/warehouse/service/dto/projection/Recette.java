package com.kobe.warehouse.service.dto.projection;

import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import java.math.BigDecimal;

public interface Recette {
    BigDecimal getMontantPaye();

    BigDecimal getMontantReel();

    ModePaimentCode getModePaimentCode();

    String getModePaimentLibelle();
}
