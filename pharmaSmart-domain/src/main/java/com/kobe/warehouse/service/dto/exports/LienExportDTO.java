package com.kobe.warehouse.service.dto.exports;

/** Un export existant, rattaché au catalogue : on y va par sa route (et son onglet). */
public record LienExportDTO(String rubrique, String libelle, String description, String route, String onglet) {}
