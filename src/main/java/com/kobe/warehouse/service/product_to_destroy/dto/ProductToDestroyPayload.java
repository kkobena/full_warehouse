package com.kobe.warehouse.service.product_to_destroy.dto;

import static java.util.Objects.isNull;

import com.kobe.warehouse.service.errors.GenericError;
import java.time.LocalDate;

public record ProductToDestroyPayload(
    Integer lotId,
    Integer produitId,
    int quantity,
    LocalDate datePeremption,
    Integer fournisseurId,
    String numLot,
    boolean editing,
    Integer magasinId,
    Integer id,
    Integer stockInitial
) {
    public ProductToDestroyPayload {
        if (quantity < 1) {
            throw new GenericError("La quantité doit être supérieure à 0");
        }
        if (isNull(datePeremption) && isNull(lotId) && isNull(id)) {
            throw new GenericError("La date de péremption est obligatoire");
        }
    }
}
