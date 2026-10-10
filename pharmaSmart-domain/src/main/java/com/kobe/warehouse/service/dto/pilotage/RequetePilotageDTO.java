package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kobe.warehouse.domain.enumeration.Granularite;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.TypeComparaison;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Ce que demande un écran du pilotage : période, comparaison, découpage, indicateurs. Les absents prennent la valeur par
 * défaut du plan : même période N-1, à date, découpage mensuel, tous les indicateurs.
 *
 * @param anneesEnArriere k pour {@link TypeComparaison#ANNEE_N_MOINS_K} (1 à 5)
 * @param referenceDu début de la référence pour {@link TypeComparaison#PERSONNALISEE}
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record RequetePilotageDTO(
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
    TypeComparaison comparaison,
    Integer anneesEnArriere,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDu,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceAu,
    Boolean aDate,
    Granularite granularite,
    List<IndicateurPilotage> indicateurs
) {
    public RequetePilotageDTO {
        comparaison = comparaison == null ? TypeComparaison.MEME_PERIODE_N_1 : comparaison;
        anneesEnArriere = anneesEnArriere == null ? 1 : anneesEnArriere;
        aDate = aDate == null || aDate;
        granularite = granularite == null ? Granularite.MOIS : granularite;
        indicateurs = indicateurs == null || indicateurs.isEmpty() ? Arrays.asList(IndicateurPilotage.values()) : List.copyOf(indicateurs);
    }
}
