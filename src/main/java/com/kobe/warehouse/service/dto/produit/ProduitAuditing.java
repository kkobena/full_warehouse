package com.kobe.warehouse.service.dto.produit;

import com.kobe.warehouse.domain.enumeration.AjustType;
import com.kobe.warehouse.domain.enumeration.TypeDeconditionnement;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProduitAuditing(
    AuditType auditType,
    Integer qtyMvt,
    Integer beforeStock,
    Integer afterStock,
    LocalDateTime updated,
    LocalDate mvtDate,
    Boolean canceled,
    String saleType,
    TypeDeconditionnement typeDeconditionnement,
    AjustType ajustType) {}
