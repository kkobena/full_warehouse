package com.kobe.warehouse.service.stock.dto;

/**
 * Représente la quantité disponible d'un lot dans un emplacement de stockage précis.
 * Un lot peut exister dans plusieurs LotStockLocation (multi-site / multi-stockage).
 */
public record LotLocationDTO(
    Integer storageId,
    String storageName,
    int qty
) {}

