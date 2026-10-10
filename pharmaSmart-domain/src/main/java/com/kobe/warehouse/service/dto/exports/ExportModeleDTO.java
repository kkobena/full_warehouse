package com.kobe.warehouse.service.dto.exports;

import com.kobe.warehouse.domain.enumeration.ExportDonnees;
import com.kobe.warehouse.domain.enumeration.FormatExport;
import com.kobe.warehouse.domain.enumeration.PeriodeRelative;
import com.kobe.warehouse.domain.enumeration.ScheduledReportFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Modèle d'export ; {@code frequence} vide : non programmé.
 *
 * @param jour jour de la semaine (1 = lundi) ou du mois (1 à 28), selon la fréquence
 * @param modifiable l'utilisateur courant en est le propriétaire
 */
public record ExportModeleDTO(
    Long id,
    @NotBlank @Size(max = 100) String libelle,
    @NotNull ExportDonnees export,
    @NotNull FormatExport format,
    PeriodeRelative periode,
    boolean partage,
    ScheduledReportFrequency frequence,
    LocalTime heure,
    Integer jour,
    LocalDateTime prochaineExecution,
    String proprietaire,
    boolean modifiable
) {}
