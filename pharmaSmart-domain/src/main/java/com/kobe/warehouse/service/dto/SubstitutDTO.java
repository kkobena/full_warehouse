package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Substitut;
import com.kobe.warehouse.service.dto.builder.ProduitBuilder;

public class SubstitutDTO {

    private final ProduitDTO produit;
    private final String typeSubstitut;
    private final String typeSubstitutLibelle;

    public SubstitutDTO(Substitut substitut) {
        this.produit = ProduitBuilder.fromProduit(substitut.getSubstitut());
        this.typeSubstitut = substitut.getType().name();
        this.typeSubstitutLibelle = substitut.getType().getLibelle();
    }

    public ProduitDTO getProduit() {
        return produit;
    }

    public String getTypeSubstitut() {
        return typeSubstitut;
    }

    public String getTypeSubstitutLibelle() {
        return typeSubstitutLibelle;
    }
}
