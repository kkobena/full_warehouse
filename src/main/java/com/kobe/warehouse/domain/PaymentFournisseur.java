package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
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

    @ManyToOne
    private Commande commande;

    public Commande getCommande() {
        return commande;
    }

    public void setCommande(Commande commande) {
        this.commande = commande;
    }
}
