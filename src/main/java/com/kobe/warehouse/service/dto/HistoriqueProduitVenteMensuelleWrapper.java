package com.kobe.warehouse.service.dto;

import java.time.Month;
import java.time.Year;
import java.util.Map;

public record HistoriqueProduitVenteMensuelleWrapper(Integer annee, Map<Month, Integer> quantites) {}
