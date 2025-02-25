package com.kobe.warehouse.service.dto.projection;

import java.math.BigDecimal;

public interface Recette {
    String getLibelle();

    BigDecimal getMontant();
}
