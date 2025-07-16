package com.kobe.warehouse.service.product_to_destroy.dto;

import java.time.LocalDate;

public record ProductToDestroyFilter(
    LocalDate fromDate,
    LocalDate toDate,
    Boolean destroyed,
    Long userId,
    Long rayonId,
    Long fournisseurId,
    String searchTerm,
    Long magasinId,
    Boolean editing
) {}
