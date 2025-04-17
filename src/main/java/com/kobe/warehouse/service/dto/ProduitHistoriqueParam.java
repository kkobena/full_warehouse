package com.kobe.warehouse.service.dto;

import java.time.LocalDate;

public record ProduitHistoriqueParam(long produitId, LocalDate startDate, LocalDate endDate) {}
