package com.kobe.warehouse.service.dashboard;

import com.kobe.warehouse.service.dto.dashboard.*;

import java.util.List;

public interface ResponsableCommandeDashboardService {

    /**
     * Récupère les données complètes du dashboard
     */
    ResponsableCommandeDashboardDTO getDashboardData();

    /**
     * Récupère les alertes stock critiques
     */
    StockAlertsDTO getStockAlerts();

    /**
     * Récupère les commandes en cours
     */
    CommandesEnCoursDTO getCommandesEnCours();

    /**
     * Récupère les produits proches de la péremption
     */
    PeremptionsDTO getPeremptions();

    /**
     * Calcule le taux de rotation du stock
     */
    RotationStockDTO getRotationStock();

    /**
     * Génère des suggestions automatiques de réapprovisionnement
     */
    List<SuggestionReapproDTO> getSuggestionsReappro();

    /**
     * Génère l'analyse ABC (Pareto) du stock
     */
    AnalyseABCDTO getAnalyseABC();

    /**
     * Récupère les performances des fournisseurs
     */
    List<PerformanceFournisseurDTO> getPerformanceFournisseurs(Integer top);
}
