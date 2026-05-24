package com.kobe.warehouse.service.dto.produit;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProduitSaleAuditing(
    boolean canceled,
    String saleType,
    int quantityRequested,
    int quantitySold,
    int quantityAvoir,
    int beforeStock,
    int afterStock,
    LocalDateTime updated,
    LocalDate mvtDate
) {}
