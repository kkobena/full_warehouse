package com.kobe.warehouse.service.dto.report;

public record ClientRetentionKpiDTO(
    long totalClients,
    long clientsActifs,
    long clientsARisque,
    long clientsPerdus,
    long caMoyenParClient
) {
    public double tauxActifs() {
        return totalClients > 0 ? (clientsActifs * 100.0) / totalClients : 0;
    }

    public double tauxRisque() {
        return totalClients > 0 ? (clientsARisque * 100.0) / totalClients : 0;
    }

    public double tauxPerdus() {
        return totalClients > 0 ? (clientsPerdus * 100.0) / totalClients : 0;
    }
}
