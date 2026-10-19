package com.kobe.warehouse.service.dto.exports;

import com.kobe.warehouse.domain.enumeration.FiltreExport;
import com.kobe.warehouse.domain.enumeration.FormatExport;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Un export à générer ; la période ne sert qu'aux entrées périodiques.
 *
 * @param export code de l'entrée du catalogue, export de données ou édition
 * @param filtres valeurs retenues par réglage, celles-là mêmes que proposent les écrans des données exportées ; les réglages que
 *     l'entrée n'annonce pas sont ignorés
 */
public record DemandeExportDTO(
    @NotBlank String export,
    @NotNull FormatExport format,
    LocalDate du,
    LocalDate au,
    Map<FiltreExport, List<String>> filtres
) {
    public DemandeExportDTO {
        filtres = filtres == null ? Map.of() : filtres;
    }
}
