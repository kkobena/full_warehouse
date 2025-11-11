package com.kobe.warehouse.service.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record RemiseProduitsDTO(@NotNull String codeRemise, Integer rayonId, Set<Integer> produitIds, String search, boolean all) {}
