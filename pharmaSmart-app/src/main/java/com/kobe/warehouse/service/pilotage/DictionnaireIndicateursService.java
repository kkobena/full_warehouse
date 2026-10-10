package com.kobe.warehouse.service.pilotage;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.DroitPilotage;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.service.dto.pilotage.AxeAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.IndicateurPilotageDTO;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/** Dictionnaire des indicateurs du pilotage. */
public interface DictionnaireIndicateursService {
    /** Les indicateurs que l'utilisateur courant a le droit de voir, dans l'ordre du dictionnaire. */
    List<IndicateurPilotageDTO> listerIndicateursAutorises();

    /** Ne garde, des indicateurs demandés, que ceux que l'utilisateur courant a le droit de voir. */
    List<IndicateurPilotage> filtrerAutorises(List<IndicateurPilotage> demandes);

    /** Droits du pilotage accordés à l'utilisateur courant. */
    Set<DroitPilotage> lireDroitsAccordes();

    /** Les axes de ventilation que l'utilisateur courant a le droit d'utiliser (le vendeur exige « Clients & équipe »). */
    List<AxeAnalyseDTO> listerAxesAutorises();

    /** Refuse (403) une analyse qui ventile ou filtre par un axe sans droit. */
    void verifierAxesAutorises(Collection<AxeAnalyse> axes);
}
