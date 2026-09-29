package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.ProduitDci;
import java.math.BigDecimal;

/** Molécule d'un produit ; en écriture, seul {@code dciId} compte, l'ordre de la liste fixe le rang. */
public record ProduitDciDTO(Integer dciId, String code, String libelle, Integer rang, BigDecimal dosageValeur, String dosageUnite) {
    public ProduitDciDTO(ProduitDci lien) {
        this(
            lien.getDci().getId(),
            lien.getDci().getCode(),
            lien.getDci().getLibelle(),
            lien.getRang(),
            lien.getDosageValeur(),
            lien.getDosageUnite()
        );
    }
}
