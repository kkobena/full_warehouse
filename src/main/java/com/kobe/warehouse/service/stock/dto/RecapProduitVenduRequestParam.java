package com.kobe.warehouse.service.stock.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record RecapProduitVenduRequestParam(
    @NotNull
    LocalDate startDate,
    @NotNull
    LocalDate endDate,
    LocalTime startTime,
    LocalTime endTime,
    Integer userId,
    String searchTerm,
    Integer rayonId,
    Integer fournisseurId,
    SeuilFilterType seuilFilterType,
    StockFilterType stockFilterType,
    Integer seuilValue,
    Integer stockValue,
    Integer quantitySold,
    Boolean unitPriceLessThanPurchasePrice,
    Boolean suggerQuantitySold,

    /**
     * uniquement pour la creation d'inventaire
     */
    Integer storage,
    Integer rayon,
    String inventoryCategory,
    Integer famillyId,
    String description
) {
}
