package com.kobe.warehouse.service.dto.records;

import java.time.LocalDate;
import java.util.List;

/**
 * Données du dashboard inventaire tournant.
 */
public record TournantDashboardRecord(
    /** Nombre total de plannings actifs */
    int nbPlanningsActifs,
    /** Nombre d'inventaires tournants créés ce mois */
    int nbInventairesCeMois,
    /** Taux de couverture = nb rayons/familles/classes déjà comptés ce mois / total */
    int tauxCouverturePct,
    /** Plannings dont la prochaine exécution est dans les 7 prochains jours */
    List<PlanningInventaireTournantRecord> prochainesExecutions,
    /** Date du prochain inventaire tournant planifié */
    LocalDate prochaineTournant
) {}
