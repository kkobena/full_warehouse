package com.kobe.warehouse.service.dto.exports;

import com.kobe.warehouse.domain.enumeration.FiltreExport;
import com.kobe.warehouse.domain.enumeration.FormatExport;
import com.kobe.warehouse.domain.enumeration.PeriodeRelative;
import com.kobe.warehouse.domain.enumeration.ScheduledReportFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Modèle d'export ; {@code frequence} vide : non programmé.
 *
 * @param jour jour de la semaine (1 = lundi) ou du mois (1 à 28), selon la fréquence
 * @param export code de l'entrée du catalogue ; une édition ne se programme pas
 * @param filtres filtres enregistrés avec le modèle, rejoués à chaque exécution
 * @param modifiable l'utilisateur courant en est le propriétaire
 */
public record ExportModeleDTO(
    Long id,
    @NotBlank @Size(max = 100) String libelle,
    @NotBlank String export,
    @NotNull FormatExport format,
    PeriodeRelative periode,
    boolean partage,
    ScheduledReportFrequency frequence,
    LocalTime heure,
    Integer jour,
    LocalDateTime prochaineExecution,
    String proprietaire,
    boolean modifiable,
    Map<FiltreExport, List<String>> filtres
) {
    public ExportModeleDTO {
        filtres = filtres == null ? Map.of() : filtres;
    }
}
