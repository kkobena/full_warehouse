package com.kobe.warehouse.service.dto.produit;

import java.time.LocalDate;

public record ProduitAuditingParam(long produitId, LocalDate fromDate, LocalDate toDate) {}
