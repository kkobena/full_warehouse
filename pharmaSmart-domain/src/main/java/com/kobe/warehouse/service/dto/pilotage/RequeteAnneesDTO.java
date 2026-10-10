package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.ModeAnnees;
import java.util.List;

/**
 * Ce que demande l'onglet « Comparer les années ». On compare des années civiles (exercice OHADA), indépendamment de la
 * période de la barre ; seul « à date » en vient.
 *
 * @param annees nombre d'années comparées, l'année en cours comprise (2 à 6)
 * @param parJourOuvre un indicateur additif rapporté au nombre de jours ouvrés (un mois à 27 jours contre 25 se compare juste)
 * @param aDate les années des tableaux croisés s'arrêtent au même jour que l'année en cours
 * @param filtre restriction au format {@code AXE:cle} (une famille, un type de vente)
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record RequeteAnneesDTO(IndicateurPilotage indicateur, Integer annees, ModeAnnees mode, Boolean parJourOuvre, Boolean aDate, List<String> filtre) {
    public RequeteAnneesDTO {
        indicateur = indicateur == null ? IndicateurPilotage.CA_TTC : indicateur;
        annees = annees == null ? 3 : annees;
        mode = mode == null ? ModeAnnees.MENSUEL : mode;
        parJourOuvre = parJourOuvre != null && parJourOuvre;
        aDate = aDate == null || aDate;
        filtre = filtre == null ? List.of() : List.copyOf(filtre);
    }

    public List<FiltreAnalyseDTO> lireFiltres() {
        return new RequeteAnalyseDTO(null, null, null, null, filtre).lireFiltres();
    }
}
