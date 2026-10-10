package com.kobe.warehouse.service.pilotage.alertes;

import com.kobe.warehouse.domain.enumeration.DroitPilotage;
import com.kobe.warehouse.service.dto.pilotage.AlertePilotageDTO;
import java.time.LocalDate;
import java.util.List;

/** Une règle d'alerte du pilotage : évaluée seulement si l'utilisateur a le droit de l'onglet qu'elle concerne. */
public interface RegleAlerte {
    DroitPilotage lireDroit();

    List<AlertePilotageDTO> evaluer(LocalDate aujourdhui);
}
