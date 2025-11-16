package com.kobe.warehouse.service.dto;

import java.time.LocalDate;

public record ProduitHistoriqueParam(Integer produitId, LocalDate startDate, LocalDate endDate, TemporalEnum groupBy) {}
