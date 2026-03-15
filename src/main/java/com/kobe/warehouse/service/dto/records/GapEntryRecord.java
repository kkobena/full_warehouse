package com.kobe.warehouse.service.dto.records;

/**
 * Entrée de qualification d'un écart soumise par le frontend.
 */
public record GapEntryRecord(
    Long lineId,
    String cause,
    String commentaire
) {}
