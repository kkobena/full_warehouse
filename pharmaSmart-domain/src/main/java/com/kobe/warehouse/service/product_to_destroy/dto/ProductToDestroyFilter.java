package com.kobe.warehouse.service.product_to_destroy.dto;

import java.time.LocalDate;

public record ProductToDestroyFilter(
    LocalDate fromDate,
    LocalDate toDate,
    Boolean destroyed,
    Integer userId,
    Integer rayonId,
    Integer fournisseurId,
    String searchTerm,
    Integer magasinId,
    Boolean editing
) {}
