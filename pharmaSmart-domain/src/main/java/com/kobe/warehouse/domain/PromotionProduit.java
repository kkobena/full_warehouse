package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Un article du périmètre d'une promotion ; {@code prixPromo} ne sert qu'au prix fixe. */
@Embeddable
public class PromotionProduit implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @Column(name = "prix_promo")
    private Integer prixPromo;

    /** Unités de l'article dans un pack ; 1 pour les autres mécaniques. */
    @Column(name = "quantite", nullable = false)
    private int quantite = 1;

    public PromotionProduit() {}

    public PromotionProduit(Produit produit, Integer prixPromo) {
        this(produit, prixPromo, 1);
    }

    public PromotionProduit(Produit produit, Integer prixPromo, int quantite) {
        this.produit = produit;
        this.prixPromo = prixPromo;
        this.quantite = Math.max(1, quantite);
    }

    public int getQuantite() {
        return quantite;
    }

    public Produit getProduit() {
        return produit;
    }

    public Integer getPrixPromo() {
        return prixPromo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PromotionProduit that)) {
            return false;
        }
        return produit != null && that.produit != null && Objects.equals(produit.getId(), that.produit.getId());
    }

    @Override
    public int hashCode() {
        return produit == null ? 0 : Objects.hashCode(produit.getId());
    }
}
