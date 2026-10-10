package com.kobe.warehouse.service.pilotage;

import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.service.dto.pilotage.GrilleObjectifsDTO;
import com.kobe.warehouse.service.dto.pilotage.SaisieObjectifsDTO;
import com.kobe.warehouse.service.dto.pilotage.SuiviObjectifsDTO;
import java.time.LocalDate;
import java.util.List;

/** Onglet Objectifs : saisie mensuelle, proposition d'après N-1, suivi et projection de fin de mois. */
public interface ObjectifsPilotageService {
    GrilleObjectifsDTO lireGrille(int annee, LocalDate aujourdhui);

    /** Un mois clos (avant le mois en cours) ne se modifie plus : la saisie qui en change la valeur est refusée en entier. */
    GrilleObjectifsDTO enregistrer(SaisieObjectifsDTO saisie, LocalDate aujourdhui);

    /** 12 valeurs d'après le même mois N-1 : × (1 + hausse %) pour un montant, la valeur N-1 telle quelle pour un taux. */
    List<Double> proposer(int annee, IndicateurPilotage indicateur, double hausse);

    SuiviObjectifsDTO suivre(int annee, LocalDate aujourdhui);
}
