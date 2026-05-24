package com.kobe.warehouse.service.dto.report;

public record RemisesAnalysisKpiDTO(
    long totalRemise,
    long caApresRemise,
    double tauxRemise,
    int nbVentesAvecRemise,
    int nbVentesTotal
) {}
