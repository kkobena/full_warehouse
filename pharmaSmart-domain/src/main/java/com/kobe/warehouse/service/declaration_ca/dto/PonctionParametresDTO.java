package com.kobe.warehouse.service.declaration_ca.dto;

import java.math.BigDecimal;

/** Les valeurs d'officine que l'ecran de ponction affiche avant toute saisie. */
public record PonctionParametresDTO(BigDecimal plafondDefaut, int delaiAnnulationJours) {}
