package com.kobe.warehouse.service.dto.produit;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProduitDeleveryAuditing(
    int beforeStock,
    int afterStock,
    int quantityRequested,
    int quantityReceived,
    int quantityUg,
    LocalDateTime updated,
    LocalDate mvtDate) {}
