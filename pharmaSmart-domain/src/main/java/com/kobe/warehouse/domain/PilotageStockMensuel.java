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
 * Stock (UG comprises) en fin de mois par produit × magasin, valorisé à la date par fn_stock_valuation_bulk.
 *
 * <p>Écrite par la fonction SQL de recalcul (migration V2.1.34), jamais par l'application.
 */
@Entity
@Immutable
@Table(name = "pilotage_stock_mensuel")
public class PilotageStockMensuel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    @Column(name = "mois")
    private LocalDate mois;

    @Column(name = "produit_id")
    private Integer produitId;

    @Column(name = "magasin_id")
    private Integer magasinId;

    @Column(name = "quantite")
    private long quantite;

    @Column(name = "valeur_achat")
    private long valeurAchat;

    @Column(name = "valeur_vente")
    private long valeurVente;

    public Long getId() {
        return id;
    }

    public LocalDate getMois() {
        return mois;
    }

    public Integer getProduitId() {
        return produitId;
    }

    public Integer getMagasinId() {
        return magasinId;
    }

    public long getQuantite() {
        return quantite;
    }

    public long getValeurAchat() {
        return valeurAchat;
    }

    public long getValeurVente() {
        return valeurVente;
    }
}
