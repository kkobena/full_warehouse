package com.kobe.warehouse.service.customer;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/** Déclaration ou modification d'un traitement chronique ; {@code actif = false} l'arrête sans l'effacer. */
public record TraitementChroniqueSaisieDTO(
    Integer dciId,
    Integer produitId,
    String dosage,
    String posologie,
    @NotNull @Positive Integer dureeJours,
    LocalDate dateOrdonnance,
    LocalDate dateFinOrdonnance,
    String note,
    Boolean actif
) {}
