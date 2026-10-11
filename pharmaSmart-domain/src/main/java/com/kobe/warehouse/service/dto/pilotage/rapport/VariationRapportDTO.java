package com.kobe.warehouse.service.dto.pilotage.rapport;

/** Variation mise en forme (« + 4,2 % », « - 1,3 pt », « n.s. ») et sa classe CSS : favorable, defavorable ou neutre. */
public record VariationRapportDTO(String texte, String appreciation) {}
