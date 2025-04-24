package com.kobe.warehouse.service.dto;

import java.time.Month;
import java.util.Map;

public record HistoriqueProduitAchatMensuelleWrapper(Integer annee, Map<Month, Integer> quantites) {}
