package com.kobe.warehouse.service.product_to_destroy.dto;

import static org.springframework.util.StringUtils.hasText;

import com.kobe.warehouse.service.errors.GenericError;
import java.time.LocalDate;

public record AddPerimePayload(Long produitId, String numLot, int quantity, LocalDate datePeremption, Long fournisseurId) {
    public AddPerimePayload {
        if (quantity < 1) {
            throw new GenericError("La quantité doit être supérieure à 0");
        }
        if (!hasText(numLot)) {
            throw new GenericError("Le numéro de lot est obligatoire");
        }
    }
}
