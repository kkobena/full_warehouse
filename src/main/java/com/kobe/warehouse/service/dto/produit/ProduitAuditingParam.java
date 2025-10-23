package com.kobe.warehouse.service.dto.produit;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ProduitAuditingParam(@NotNull long produitId, LocalDate fromDate, LocalDate toDate,Long magasinId) {}
