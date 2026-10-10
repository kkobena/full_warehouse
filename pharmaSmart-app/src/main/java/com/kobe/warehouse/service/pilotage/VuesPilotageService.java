package com.kobe.warehouse.service.pilotage;

import com.kobe.warehouse.service.dto.pilotage.VuePilotageDTO;
import java.util.List;

/** Vues enregistrées de l'onglet Analyser : livrées d'office, personnelles, partagées avec l'équipe. */
public interface VuesPilotageService {
    /** Les vues livrées, puis celles de l'utilisateur courant et celles que l'équipe partage. */
    List<VuePilotageDTO> listerVues();

    /** Crée la vue (sans id) ou modifie celle de l'utilisateur courant ; une vue livrée ou d'un collègue n'est pas modifiable. */
    VuePilotageDTO enregistrerVue(VuePilotageDTO vue);

    void supprimerVue(Long id);
}
