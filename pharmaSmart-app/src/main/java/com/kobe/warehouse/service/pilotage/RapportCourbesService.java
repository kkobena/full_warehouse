package com.kobe.warehouse.service.pilotage;

import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.FormatGraphe;

/** Pendant serveur d'{@code app-courbe-indicateur} : un indicateur sur les 13 dernières tranches, sa référence et son objectif. */
public interface RapportCourbesService {
    /**
     * @param contreObjectif la référence est l'objectif (comparaison « Objectifs ») : pas de seconde ligne d'objectif
     * @return l'image en data URI, {@code null} sans donnée
     */
    String dessinerHistorique(RequetePilotageDTO requete, IndicateurPilotage indicateur, boolean contreObjectif, FormatGraphe format);

    /** La même courbe sur la seule période de la requête, à son découpage (les ventes par jour de la fréquentation). */
    String dessinerPeriode(RequetePilotageDTO requete, IndicateurPilotage indicateur, FormatGraphe format);
}
