package com.kobe.warehouse.service.dto.pilotage;

import com.kobe.warehouse.domain.enumeration.AffichageAnalyse;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import java.util.List;

/**
 * Le réglage de l'onglet Analyser tel que l'écran l'affiche, pour le rapport PDF.
 *
 * @param indicateurs un à trois ; le premier pilote le graphique, la part et la contribution
 * @param libellesChemin libellés des étapes de descente, dans l'ordre de {@code analyse.filtre}, pour le fil imprimé
 */
public record ReglageAnalyseRapportDTO(
    List<IndicateurPilotage> indicateurs,
    RequeteAnalyseDTO analyse,
    AffichageAnalyse affichage,
    List<String> libellesChemin
) {
    public ReglageAnalyseRapportDTO {
        indicateurs = indicateurs == null || indicateurs.isEmpty() ? List.of(IndicateurPilotage.CA_TTC) : List.copyOf(indicateurs);
        analyse = analyse == null ? new RequeteAnalyseDTO(null, null, null, null, null) : analyse;
        affichage = affichage == null ? AffichageAnalyse.BARRES : affichage;
        libellesChemin = libellesChemin == null ? List.of() : List.copyOf(libellesChemin);
    }
}
