package com.kobe.warehouse.service.reglement.differe.dto;

import com.kobe.warehouse.domain.enumeration.ModePaimentCode;

import java.util.Objects;
import java.util.Set;

public record NewDifferePaymentDTO(long customerId, Set<Long> saleIds,int expectedAmount, int amount, ModePaimentCode paimentMode) {
    public NewDifferePaymentDTO {
        if (amount <= 0) {
            throw new IllegalArgumentException("Le montant doit être supérieur à zéro");
        }
        if (Objects.isNull(paimentMode)) {
            throw new IllegalArgumentException("Le mode de paiement ne doit pas être nul");
        }
        if (Objects.isNull(saleIds) || saleIds.isEmpty()) {
            throw new IllegalArgumentException("La liste des ventes ne doit pas être vide");
        }
        if (customerId <= 0) {
            throw new IllegalArgumentException("Donnée client invalide");
        }
    }
}
