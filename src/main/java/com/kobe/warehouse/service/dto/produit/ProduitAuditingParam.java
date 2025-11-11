package com.kobe.warehouse.service.dto.produit;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ProduitAuditingParam(@NotNull Integer produitId, LocalDate fromDate, LocalDate toDate,Integer magasinId) {}
