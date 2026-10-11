package com.kobe.warehouse.service.dto.pilotage;

import com.kobe.warehouse.domain.enumeration.OngletRapportPilotage;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Rapport PDF du pilotage : la requête de la barre de période, les onglets à imprimer, et le réglage propre des onglets qui en
 * ont un (celui de l'écran ; absent, le réglage par défaut).
 *
 * @param libelleFiltreAnnees nom de la famille ou du type de vente filtré dans « Comparer les années », pour l'en-tête
 * @param analyse réglage de l'onglet Analyser
 */
public record RequeteRapportPilotageDTO(
    @NotNull RequetePilotageDTO requete,
    @NotEmpty List<OngletRapportPilotage> onglets,
    RequeteAnneesDTO annees,
    String libelleFiltreAnnees,
    ReglageAnalyseRapportDTO analyse
) {
    public RequeteRapportPilotageDTO {
        analyse = analyse == null ? new ReglageAnalyseRapportDTO(null, null, null, null) : analyse;
    }

    /** « À date » vient de la barre, comme à l'écran. */
    public RequeteAnneesDTO lireReglageAnnees() {
        RequeteAnneesDTO reglage = annees == null ? new RequeteAnneesDTO(null, null, null, null, null, null) : annees;
        return new RequeteAnneesDTO(reglage.indicateur(), reglage.annees(), reglage.mode(), reglage.parJourOuvre(), requete.aDate(), reglage.filtre());
    }
}
