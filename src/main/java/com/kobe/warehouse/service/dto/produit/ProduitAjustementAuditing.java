package com.kobe.warehouse.service.dto.produit;

import com.kobe.warehouse.domain.enumeration.AjustType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProduitAjustementAuditing(
    AjustType ajustType,
    int qtyMvt,
    int beforeStock,
    int afterStock,
    LocalDateTime updated,
    LocalDate mvtDate
) {}
