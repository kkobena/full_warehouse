package com.kobe.warehouse.service.dto.produit;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProduitInventoryAuditing(
    int beforeStock, int afterStock, LocalDateTime updated, LocalDate mvtDate) {}
