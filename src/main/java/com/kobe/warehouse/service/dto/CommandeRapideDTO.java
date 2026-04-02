package com.kobe.warehouse.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CommandeRapideDTO(@NotNull Integer fournisseurProduitId, @NotNull
@Min(1) Integer quantityRequested, int totalQuantity) {


}
