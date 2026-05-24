package com.kobe.warehouse.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import java.io.Serial;
import java.io.Serializable;

/**
 * A PaymentFournisseur.
 */

@Entity
public class PaymentFournisseur extends PaymentTransaction implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns(
        {
            @JoinColumn(name = "commande_id", referencedColumnName = "id"),
            @JoinColumn(name = "commande_order_date", referencedColumnName = "order_date"),
        }
    )

    private Commande commande;

    public Commande getCommande() {
        return commande;
    }

    public void setCommande(Commande commande) {
        this.commande = commande;
    }
}
