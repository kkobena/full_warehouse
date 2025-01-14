package com.kobe.warehouse.service.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record RemiseProduitsDTO(@NotNull String codeRemise, Long rayonId, Set<Long> produitIds, String search, boolean all) {}
