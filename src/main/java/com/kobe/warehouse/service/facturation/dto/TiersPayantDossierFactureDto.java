package com.kobe.warehouse.service.facturation.dto;

import java.math.BigDecimal;

public record TiersPayantDossierFactureDto(Long id, String name, BigDecimal totalAmount, int factureItemCount) {}
