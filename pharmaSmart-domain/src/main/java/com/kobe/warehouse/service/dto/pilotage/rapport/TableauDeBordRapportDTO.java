package com.kobe.warehouse.service.dto.pilotage.rapport;

import com.kobe.warehouse.service.dto.pilotage.AlertePilotageDTO;
import java.util.List;

/**
 * Onglet Tableau de bord tel que l'imprime le rapport : mêmes tuiles, courbe, écarts et tableau que l'écran.
 *
 * @param courbeCa image PNG en data URI, prête pour un {@code <img src>}
 * @param ecartCa {@code null} sans référence comparable
 */
public record TableauDeBordRapportDTO(
    String contexte,
    List<AlertePilotageDTO> alertes,
    List<TuileRapportDTO> tuiles,
    String titreCourbe,
    String courbeCa,
    MontantRapportDTO ecartCa,
    List<MontantRapportDTO> effets,
    List<MontantRapportDTO> hausses,
    List<MontantRapportDTO> baisses,
    TableauRapportDTO detail
) {}
