package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SourceAnalyse;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Lecture d'un agrégat regroupé par un ou deux axes lus en base, et par jour si demandé.
 *
 * @param axes axes lus en base, dans l'ordre des clés de {@link MesuresVentileesDTO} (0 à 2)
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record RequeteVentilationDTO(
    LocalDate du,
    LocalDate au,
    SourceAnalyse source,
    List<AxeAnalyse> axes,
    boolean parJour,
    List<FiltreAnalyseDTO> filtres,
    Set<CategorieChiffreAffaire> categories
) {}
