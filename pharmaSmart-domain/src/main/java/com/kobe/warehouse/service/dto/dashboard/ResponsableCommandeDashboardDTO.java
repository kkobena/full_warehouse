package com.kobe.warehouse.service.dto.dashboard;

import java.util.List;

public record ResponsableCommandeDashboardDTO(
    StockAlertsDTO stockAlerts,
    CommandesEnCoursDTO commandesEnCours,
    PeremptionsDTO peremptions,
    RotationStockDTO rotationStock,
    List<SuggestionReapproDTO> suggestions,
    AnalyseABCDTO analyseABC,
    List<PerformanceFournisseurDTO> performanceFournisseurs
) {
}
