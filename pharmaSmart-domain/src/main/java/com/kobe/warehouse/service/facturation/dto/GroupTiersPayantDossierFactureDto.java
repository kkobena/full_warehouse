package com.kobe.warehouse.service.facturation.dto;

import java.math.BigDecimal;

public record GroupTiersPayantDossierFactureDto(Long groupId, String groupName, BigDecimal totalAmount, int factureItemCount) {}
