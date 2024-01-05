package com.kobe.warehouse.service.dto.produit;

import com.kobe.warehouse.domain.enumeration.TypeDeconditionnement;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProduitDeconditionAuditing(
    TypeDeconditionnement typeDeconditionnement,
    int qtyMvt,
    int beforeStock,
    int afterStock,
    LocalDateTime updated,
    LocalDate mvtDate) {}
