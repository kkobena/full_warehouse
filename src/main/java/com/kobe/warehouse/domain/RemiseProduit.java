package com.kobe.warehouse.domain;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A RemiseProduit.
 */
@Entity
public class RemiseProduit extends Remise implements Serializable {

	private static final long serialVersionUID = 1L;
	@OneToMany(mappedBy = "remise")
    private Set<Produit> produits = new HashSet<>();
    public Set<Produit> getProduits() {
        return produits;
    }

    public RemiseProduit produits(Set<Produit> produits) {
        this.produits = produits;
        return this;
    }



    public void setProduits(Set<Produit> produits) {
        this.produits = produits;
    }


}
