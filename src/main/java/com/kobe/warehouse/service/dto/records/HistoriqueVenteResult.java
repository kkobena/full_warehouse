package com.kobe.warehouse.service.dto.records;

import com.kobe.warehouse.service.dto.HistoriqueProduitVente;

import java.util.List;

public record HistoriqueVenteResult(Integer totalElements, List<HistoriqueProduitVente> content) {
}
