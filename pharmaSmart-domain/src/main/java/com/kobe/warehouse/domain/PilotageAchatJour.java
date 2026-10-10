package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import org.hibernate.annotations.Immutable;

/**
 * Commandes reçues, datées à la réception, agrégées par jour × produit × fournisseur. Le prix d'achat est TTC ; le HT
 * retire la taxe portée par la ligne.
 *
 * <p>Écrite par la fonction SQL de recalcul (migration V2.1.34), jamais par l'application.
 */
@Entity
@Immutable
@Table(name = "pilotage_achat_jour")
public class PilotageAchatJour implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    @Column(name = "jour")
    private LocalDate jour;

    @Column(name = "produit_id")
    private Integer produitId;

    @Column(name = "fournisseur_id")
    private Integer fournisseurId;

    @Column(name = "nb_bons")
    private long nbBons;

    @Column(name = "quantite_commandee")
    private long quantiteCommandee;

    @Column(name = "quantite_recue")
    private long quantiteRecue;

    @Column(name = "quantite_gratuite")
    private long quantiteGratuite;

    @Column(name = "montant_ht")
    private long montantHt;

    @Column(name = "montant_ttc")
    private long montantTtc;

    public Long getId() {
        return id;
    }

    public LocalDate getJour() {
        return jour;
    }

    public Integer getProduitId() {
        return produitId;
    }

    public Integer getFournisseurId() {
        return fournisseurId;
    }

    public long getNbBons() {
        return nbBons;
    }

    public long getQuantiteCommandee() {
        return quantiteCommandee;
    }

    public long getQuantiteRecue() {
        return quantiteRecue;
    }

    public long getQuantiteGratuite() {
        return quantiteGratuite;
    }

    public long getMontantHt() {
        return montantHt;
    }

    public long getMontantTtc() {
        return montantTtc;
    }
}
